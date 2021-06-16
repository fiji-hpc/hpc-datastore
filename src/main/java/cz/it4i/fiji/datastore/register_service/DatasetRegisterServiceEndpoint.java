/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import cz.it4i.fiji.datastore.DataStoreException;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Path("/")
public class DatasetRegisterServiceEndpoint {


	public static final String UUID = "uuid";
	public static final String X_PARAM = "x";
	public static final String Y_PARAM = "y";
	public static final String Z_PARAM = "z";
	public static final String R_X_PARAM = "RxParam";
	public static final String R_Y_PARAM = "RyParam";
	public static final String R_Z_PARAM = "RzParam";
	public static final String VERSION_PARAM = "versionParam";
	public static final String MODE_PARAM = "mode";
	public static final String TIMEOUT_PARAM = "timeout";

	@Inject
	DatasetRegisterServiceImpl datasetRegisterServiceImpl;

//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + ":[^-]+}" 
			+ "/{" + MODE_PARAM +"}")
// @formatter:on
	@GET
	public Response startRead(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(MODE_PARAM) String modeName,
		@QueryParam(TIMEOUT_PARAM) Long timeout)
	{
		OperationMode opMode = OperationMode.getByUrlPath(modeName);

		if (opMode == OperationMode.NOT_SUPPORTED) {
			return Response.status(Status.BAD_REQUEST).entity(String.format(
				"mode (%s) not supported", modeName)).build();
		}
		try {
			URL serverURL = datasetRegisterServiceImpl.start(java.util.UUID
				.fromString(uuid), new int[] { rX, rY, rZ }, version, opMode, timeout);
			log.debug("start reading> timeout = {}", timeout);
			return Response.temporaryRedirect(serverURL.toURI()).build();
		}
		catch (DataStoreException | URISyntaxException exc) {
			log.error("Starting server", exc);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
				"Starting throws exception").build();
		}


	}

	@POST
	@Path("datasets/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEmptyDataset(DatasetDTO dataset)
	{
		log.debug("dataset=" + dataset);
		try {
			java.util.UUID result = datasetRegisterServiceImpl.createEmptyDataset(
				dataset);
			return Response.ok().entity(result.toString()).type(
				MediaType.TEXT_PLAIN).build();
		}
		catch (Exception exc) {
			log.warn("read", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
	}

	@GET
	@Path("datasets/{" + UUID + "}")
	public Response queryDataset(@PathParam(UUID) String uuid) {
		DatasetDTO result = datasetRegisterServiceImpl.query(uuid);
		if (result == null) {
			return Response.status(Status.NOT_FOUND).entity("Dataset with uuid=" +
				uuid + " not found.").build();
		}
		return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
	}
}