/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.UUID;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.VERSION_PARAM;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import cz.it4i.fiji.datastore.ApplicationConfiguration;
import cz.it4i.fiji.datastore.core.HPCDatastoreImageLoader;
import mpicbg.spim.data.SpimDataException;

@ApplicationScoped
@Path("/")
public class HPCDatastoreEndpoint {

	@Inject
	JsonDatasetListHandlerTS jsonDatasetListHandlerTS;

	@Inject
	GetSpimDataMinimalTS getSpimDataMinimalTS;

	@Inject
	ApplicationConfiguration configuration;

	private Map<String, ThumbnailProviderTS> thumbnailsGenerators =
		new HashMap<>();

	@GET
	@Path("/datasets/{" + UUID + "}/json")
	public void getJSONListDatastoreLoader(@PathParam(UUID) String uuid,
		@Context HttpServletResponse response, @Context UriInfo uriInfo)
		throws IOException
	{
		jsonDatasetListHandlerTS.run(uuid, response,
				uriInfo.getRequestUri(), true);


	}

	@GET
	@Path("datasets/{" + UUID + "}/{" + VERSION_PARAM +
		":(all|\\d+)|mixedLatest}")
	@Produces(APPLICATION_XML)
	public Response getMetadataXML(@PathParam(UUID) String uuidStr,
		@PathParam(VERSION_PARAM) String versionStr, @Context UriInfo uriInfo)
	{


		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();

		try (final StringWriter ow = new StringWriter()) {
			SpimDataMinimal spimData = getSpimDataMinimalTS.run(uuidStr, versionStr);
			BuildRemoteDatasetXmlTS.run(io, spimData, new HPCDatastoreImageLoader(uriInfo.getRequestUri().toString()), ow);
			return Response.ok(ow.toString()).build();
		}
		catch (IOException | SpimDataException exc) {
			throw new InternalServerErrorException(exc);
		}

	}

	@GET
	@Path("datasets/{" + UUID + "}/{" + VERSION_PARAM +
		":(all|\\\\d+)|mixedLatest}/png")
	public void getThumbnail(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) String version,
		@Context HttpServletResponse response) throws IOException
	{
		ThumbnailProviderTS ts = getThumbnailProvider(uuid, version);
		ts.runForThumbnail(response);
	}

	@GET
	@Path("datasets/{" + UUID + "}" + "/{" + VERSION_PARAM + "}/settings")
	//
	public Response getSettingsXML()
	{
		return Response.status(Status.NOT_FOUND).entity("settings.xml").build();
	}

	private ThumbnailProviderTS getThumbnailProvider(String uuid,
		String version)
	{
		String key = getKey(uuid, version);
		return thumbnailsGenerators.computeIfAbsent(key,
			x -> {
				try {
					return constructThumbnailGeneratorTS(uuid, version);
				}
				catch (SpimDataException | IOException exc) {
					throw new InternalServerErrorException(exc);
				}
			});
	}

	private ThumbnailProviderTS constructThumbnailGeneratorTS(String uuid,
		String version) throws SpimDataException, IOException
	{
		// thumbnail is done from mixedLatest version as it requires transform
		// setups in
		// N5Reader and get N5Reader base on setupID
		version = "mixedLatest";
		SpimDataMinimal spimData = getSpimDataMinimalTS.run(uuid, version);
		return new ThumbnailProviderTS(spimData, uuid + "_version-" + version,
			GetThumbnailsDirectoryTS.$());
	}

	private String getKey(String uuid, String version) {
		return uuid + ":" + version;
	}


}
