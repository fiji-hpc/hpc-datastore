/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import static cz.it4i.fiji.datastore.DatasetServerEndpoint.ANGLE_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.CHANNEL_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.TIME_PARAM;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.security.Authorization;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimDataException;

@Authorization
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
	public static final String VERSION_PARAMS = "versionParams";
	public static final String MODE_PARAM = "mode";
	public static final String TIMEOUT_PARAM = "timeout";
	private static final String RESOLUTION_PARAM = "resolutionParam";
	private static final Pattern URL_RESOLUTIONS_PATTERN = Pattern.compile(
		"(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)");

	@Inject
	DatasetRegisterServiceImpl datasetRegisterServiceImpl;

	@Path("/hello")
	@GET
	public Response hello() {
		return Response.ok("<h1>Hello world</h1>").build();
	}
	
	@PUT
	@POST
	@Path("datasets/{" + UUID + "}" + "/{" + VERSION_PARAM + "}" + "{" +
		VERSION_PARAMS + ":/?.*}")
	//@formatter:on
	public Response notFound(@Context UriInfo request) {
		return Response.status(Status.NOT_FOUND).entity(String.format(
			"Resource %s not found", request.getPath())).build();
	}

//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/{" + MODE_PARAM +"}")
// @formatter:on
	@GET
	public Response startDatasetServer(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(MODE_PARAM) String modeName,
		@QueryParam(TIMEOUT_PARAM) Long timeout)
	{
		log.info("starting server for " + modeName + " dataset=" + uuid);
		OperationMode opMode = OperationMode.getByUrlPath(modeName);

		if (opMode == OperationMode.NOT_SUPPORTED) {
			return Response.status(Status.BAD_REQUEST).entity(String.format(
				"mode (%s) not supported", modeName)).build();
		}
		try {
			URI serverURI = datasetRegisterServiceImpl.start(uuid, new int[] { rX, rY,
				rZ }, version, opMode, timeout);
			log.debug("start reading> timeout = {}", timeout);
			return Response.temporaryRedirect(serverURI).build();
		}
		catch (IOException exc) {
			log.error("Starting server", exc);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
				"Starting throws exception").build();
		}
	}


//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + RESOLUTION_PARAM + ":[0-9]+/[0-9]+/[0-9]+/?.*}"
			+ "/write") //TODO use write and merge with other start
// @formatter:on
	@GET
	public Response startDatasetServer(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ,
		@PathParam(RESOLUTION_PARAM) String resolutionString,
		@QueryParam(TIMEOUT_PARAM) Long timeout)
	{
		log.info("starting2 server for writing dataset=" + uuid);
		List<int[]> resolutions = getResolutions(rX, rY, rZ, resolutionString);
		try {
			URI serverURI = datasetRegisterServiceImpl.start(uuid, resolutions,
				timeout);
			log.debug("start reading> timeout = {}", timeout);
			return Response.temporaryRedirect(serverURI).build();
		}
		catch (IOException exc) {
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
		log.info("creating empty dataset");
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

	@POST
	@Path("datasets/{" + UUID + "}")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response addExistingDataset(@PathParam(UUID) String uuid)
	{
		log.info("adding existing dataset {}", uuid);
		try {
			datasetRegisterServiceImpl.addExistingDataset(uuid);
		}
		catch (IOException exc) {
			throw new NotFoundException("Dataset with uuid " + uuid +
				"  was not located in storage ");
		}
		catch (DatasetAlreadyInsertedException exc) {
			return Response.status(Status.CONFLICT).entity("Dataset with uuid " + exc
				.getUuid() + " is already added.").build();
		}
		catch (Exception exc) {
			throw new InternalServerErrorException("Cannot add dataset " + uuid);
		}
		return Response.ok().entity("Done.").build();
	}

	@GET
	@Path("datasets/{" + UUID + "}")
	public Response queryDataset(@PathParam(UUID) String uuid) {
		log.info("get JSON for dataset=" + uuid);
		DatasetDTO result;
		try {
			result = datasetRegisterServiceImpl.query(uuid);
		}
		catch (SpimDataException exc) {
			throw new InternalServerErrorException("Query to dataset failed", exc);
		}
		return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
	}

	@DELETE
	@Path("datasets/{" + UUID + "}")
	public Response deleteDataset(@PathParam(UUID) String uuid) {
		log.info("deleting dataset=" + uuid);
		try {
			datasetRegisterServiceImpl.deleteDataset(uuid);
		}
		catch (Exception exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage()).build();
		}
		return Response.ok().build();
	}
	@GET
	@Path("datasets/{" + UUID + "}/delete")
	public Response deleteDataset_viaGet(@PathParam(UUID) String uuid) {
		log.info("deleting (GET) dataset=" + uuid);
		return deleteDataset(uuid);
	}


//@formatter:off
	@DELETE
	@Path("datasets/{" + UUID + "}" +
				"/{" + VERSION_PARAM + "}"+
			  "{" + VERSION_PARAMS + ":/?.*}")
//@formatter:on
	public Response deleteDatasetVersions(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) String version,
		@PathParam(VERSION_PARAMS) String versions)
	{
		log.info("deleting versions from dataset=" + uuid);
		List<Integer> versionList = getVersions(version, versions);
		try {
			datasetRegisterServiceImpl.deleteVersions(uuid, versionList);
		}
		catch (IOException exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage()).build();
		}
		return Response.ok().build();
	}

	//@formatter:off
	@GET
	@Path("datasets/{" + UUID + "}" +
			  "/{" + VERSION_PARAM + "}"+
			  "{" + VERSION_PARAMS + ":/?.*}/delete")
//@formatter:on
	public Response deleteDatasetVersions_viaGet(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) String version,
		@PathParam(VERSION_PARAMS) String versions)
	{
		log.info("deleting (GET) versions from dataset=" + uuid);
		return deleteDatasetVersions(uuid,version,versions);
	}

	@GET
	@Path("datasets/{" + UUID + "}/common-metadata")
	public Response getCommonMetadata(@PathParam(UUID) String uuid) {
		log.info("getting common metadata from dataset=" + uuid);
		String result = datasetRegisterServiceImpl.getCommonMetadata(uuid);
		return Response.ok(result).type(MediaType.TEXT_PLAIN).build();
	}

	@POST
	@Path("datasets/{" + UUID + "}/common-metadata")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response setCommonMetadata(@PathParam(UUID) String uuid,
		String commonMetadata)
	{
		log.info("setting common metadata into dataset=" + uuid);
		datasetRegisterServiceImpl.setCommonMetadata(uuid, commonMetadata);
		return Response.ok().build();
	}

	@POST
	@Path("datasets/{" + UUID + "}/channels")
	public Response addChannels(@PathParam(UUID) String uuid,
		String strChannels)
	{
		try {
			int channels = strChannels.isEmpty() ? 1 : Integer.parseInt(strChannels);
			log.info("add channels " + channels + " for dataset=" + uuid);
			datasetRegisterServiceImpl.addChannels(uuid, channels);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException(strChannels + " is not integer");
		}
		catch (Exception exc) {
			log.warn("read", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
		return Response.ok().build();
	}

	@PUT
	@DELETE
	@Path("datasets/{" + UUID + "}/channels")
	public Response notAllowedChannels(@PathParam(UUID) String uuid) {
		log.info("not allowed method for channels of dataset=" + uuid);
		return Response.status(Status.METHOD_NOT_ALLOWED).build();
	}

	@GET
	@Path("datasets/{" + UUID + "}/channels")
	public Response getChannels(@PathParam(UUID) String uuid)
	{
		DatasetDTO result;
		try {
			result = datasetRegisterServiceImpl.query(uuid);
		}
		catch (SpimDataException exc) {
			throw new InternalServerErrorException("Query to dataset failed", exc);
		}
		if (result == null) {
			return Response.status(Status.NOT_FOUND).entity("Dataset with uuid=" +
				uuid + " not found.").build();
		}
		return Response.ok(result).entity(result.getChannels()).build();
	}

	@PATCH
	//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + TIME_PARAM + "}"
			+ "/{" + CHANNEL_PARAM + "}"
			+ "/{" + ANGLE_PARAM +		"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/rebuild")
// @formatter:on
	public Response rebuild(@PathParam(UUID) String uuid,
		@PathParam(VERSION_PARAM) int version, @PathParam(TIME_PARAM) int time,
		@PathParam(CHANNEL_PARAM) int channel, @PathParam(ANGLE_PARAM) int angle)
	{
		try {
			datasetRegisterServiceImpl.rebuild(uuid, version, time, channel, angle);
		}
		catch (IOException exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage()).build();
		}
		catch (SpimDataException exc) {
			log.error("rebuild", exc);
			throw new InternalServerErrorException(
				"Rebuild failure. Contact administrator");
		}
		return Response.status(Status.NOT_IMPLEMENTED).build();
	}

	public List<int[]> getResolutions(int rX, int rY, int rZ,
		String resolutionString)
	{
		List<int[]> resolutions = new LinkedList<>();
		resolutions.add(new int[] { rX, rY, rZ });
		extract(resolutionString, resolutions);
		return resolutions;
	}

	public static List<Integer> getVersions(String version, String versions) {
		List<Integer> versionList = new LinkedList<>();
		versionList.add(getVersion(version));
		versionList.addAll(extractVersions(versions).stream().filter(e -> !e
			.isEmpty()).map(DatasetRegisterServiceEndpoint::getVersion).collect(
				Collectors.toList()));
		return versionList;
	}

	private static void extract(String resolutionString,
		List<int[]> resolutions)
	{
		Matcher matcher = URL_RESOLUTIONS_PATTERN.matcher(resolutionString);
		while (matcher.find()) {
			resolutions.add(new int[] { getInt(matcher, 1), getInt(matcher, 2),
				getInt(matcher, 3) });
		}

	}

	private static Collection<String> extractVersions(String versions) {
		return Arrays.asList(versions.split("/"));
	}

	private static int getInt(Matcher matcher, int i) {
		return Integer.parseInt(matcher.group(i));
	}

	private static Integer getVersion(String version) {
		try {
			return Integer.parseInt(version);
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(version + " is not correct version");
		}
	}
}