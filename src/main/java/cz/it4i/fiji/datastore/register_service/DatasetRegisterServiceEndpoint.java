/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
	public static final String VERSION_PARAMS = "versionParams";
	public static final String MODE_PARAM = "mode";
	public static final String TIMEOUT_PARAM = "timeout";
	private static final String RESOLUTION_PARAM = "resolutionParam";
	private static final Pattern URL_RESOLUTIONS_PATTERN = Pattern.compile(
		"(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)");

	@Inject
	DatasetRegisterServiceImpl datasetRegisterServiceImpl;

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
		OperationMode opMode = OperationMode.getByUrlPath(modeName);

		if (opMode == OperationMode.NOT_SUPPORTED) {
			return Response.status(Status.BAD_REQUEST).entity(String.format(
				"mode (%s) not supported", modeName)).build();
		}
		try {
			URL serverURL = datasetRegisterServiceImpl.start(uuid, new int[] { rX, rY,
				rZ }, version, opMode, timeout);
			log.debug("start reading> timeout = {}", timeout);
			return Response.temporaryRedirect(serverURL.toURI()).build();
		}
		catch (IOException | URISyntaxException exc) {
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
		List<int[]> resolutions = getResolutions(rX, rY, rZ, resolutionString);
		try {
			URL serverURL = datasetRegisterServiceImpl.start(uuid, resolutions,
				timeout);
			log.debug("start reading> timeout = {}", timeout);
			return Response.temporaryRedirect(serverURL.toURI()).build();
		}
		catch (IOException | URISyntaxException exc) {
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
			+ "{" + RESOLUTION_PARAM + ":/?.*}"
			+ "/rebuild")
// @formatter:on
	@GET
	public Response rebuild(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ,
		@PathParam(RESOLUTION_PARAM) String resolutionString)
	{
		List<int[]> resolutions = getResolutions(rX, rY, rZ, resolutionString);
		try {
			datasetRegisterServiceImpl.rebuild(uuid, resolutions);
		}
		catch (IOException exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage()).build();
		}
		return Response.ok().build();
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
		DatasetDTO result;
		try {
			result = datasetRegisterServiceImpl.query(uuid);
			if (result == null) {
				return Response.status(Status.NOT_FOUND).entity("Dataset with uuid=" +
					uuid + " not found.").build();
			}
			return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
		}
		catch (IOException exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage())
				.build();
		}
	}

	@DELETE
	@Path("datasets/{" + UUID + "}")
	public Response deleteDataset(@PathParam(UUID) String uuid) {
		try {
			datasetRegisterServiceImpl.deleteDataset(uuid);
		}
		catch (IOException exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage()).build();
		}
		return Response.ok().build();
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
		List<Integer> versionList = new LinkedList<>();
		versionList.add(getVersion(version));
		versionList.addAll(extractVersions(versions).stream().filter(e -> !e
			.isEmpty()).map(DatasetRegisterServiceEndpoint::getVersion).collect(
				Collectors.toList()));
		try {
			datasetRegisterServiceImpl.deleteVersions(uuid, versionList);
		}
		catch (IOException exc) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc
				.getMessage()).build();
		}
		return Response.ok().build();
	}
	
	@GET
	@Path("datasets/{" + UUID + "}/common-metadata")
	public Response getCommonMetadata(@PathParam(UUID) String uuid) {
		String result = datasetRegisterServiceImpl.getCommonMetadata(uuid);
		return Response.ok(result).type(MediaType.TEXT_PLAIN).build();
	}

	@POST
	@Path("datasets/{" + UUID + "}/common-metadata")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response setCommonMetadata(@PathParam(UUID) String uuid,
		String commonMetadata)
	{
		datasetRegisterServiceImpl.setCommonMetadata(uuid, commonMetadata);
		return Response.ok().build();
	}

	public List<int[]> getResolutions(int rX, int rY, int rZ,
		String resolutionString)
	{
		List<int[]> resolutions = new LinkedList<>();
		resolutions.add(new int[] { rX, rY, rZ });
		extract(resolutionString, resolutions);
		return resolutions;
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