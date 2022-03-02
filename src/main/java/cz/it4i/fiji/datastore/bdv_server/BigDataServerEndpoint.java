/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import static cz.it4i.fiji.datastore.core.Version.stringToIntVersion;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.UUID;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.VERSION_PARAM;

import com.google.common.base.Strings;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@ApplicationScoped
@Path("/bdv")
public class BigDataServerEndpoint {

	private static final String P_PARAM = "p";

	@Inject
	JsonDatasetListHandlerTS jsonDatasetListHandlerTS;
	
	@Inject
	CellHandlerTSProducer cellHandlerTSProducer;

	@Context
	UriInfo uri;

	final private Map<String, CellHandlerTS> cellHandlersTS = new HashMap<>();

	@GET
	@Path("{" + UUID + "}/json")
	public void getJSONList(@PathParam(UUID) String uuid,
		@Context HttpServletResponse response) throws IOException
	{
		jsonDatasetListHandlerTS.run(uuid, response,
			uri.getRequestUri());
	}

	@GET
	@Path("{" + UUID + "}/{" + VERSION_PARAM + "}")
	public Response getCell(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) String version,
		@QueryParam(P_PARAM) String cellString)
	{
		CellHandlerTS ts = getCellHandler(uuid, version);
		if (Strings.emptyToNull(cellString) == null) {
			return ts.runForDataset();
		}
		return ts.runForCellOrInit(cellString);
	}

	@GET
	@Path("{" + UUID + "}/{" + VERSION_PARAM + "}/settings")
	public Response getSettings(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) String version)
	{
		CellHandlerTS ts = getCellHandler(uuid, version);
		return ts.runForSettings();
	}

	@GET
	@Path("{" + UUID + "}/{" + VERSION_PARAM + "}/png")
	public void getThumbnail(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) String version,
		@Context HttpServletResponse response) throws IOException
	{
		CellHandlerTS ts = getCellHandler(uuid, version);
		ts.runForThumbnail(response);
	}

	private CellHandlerTS getCellHandler(String uuidStr,
		final String versionStr)
	{
		final int version = stringToIntVersion(versionStr);
		String key = getKey(uuidStr, versionStr);
		URI baseURI = uri.getBaseUri();
		String baseURL = baseURI.resolve("bdv/").resolve(uuidStr + "/").resolve(
			versionStr).toString();
		return cellHandlersTS.computeIfAbsent(key, x -> cellHandlerTSProducer
			.produce(baseURL, uuidStr, version));
	}


	private String getKey(String uuid, String version) {
		return uuid + ":" + version;
	}
}
