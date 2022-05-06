/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetServerEndpoint.ANGLE_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.BLOCKS_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.CHANNEL_PARAM;
import static cz.it4i.fiji.datastore.DatasetServerEndpoint.TIME_PARAM;
import static cz.it4i.fiji.datastore.core.Version.stringToIntVersion;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.R_Z_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.UUID;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.VERSION_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.X_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Y_PARAM;
import static cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint.Z_PARAM;
import static cz.it4i.fiji.datastore.register_service.OperationMode.READ;
import static cz.it4i.fiji.datastore.register_service.OperationMode.READ_WRITE;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import cz.it4i.fiji.datastore.DatasetServerEndpoint.RootResponse;
import cz.it4i.fiji.datastore.core.Version;
import cz.it4i.fiji.datastore.register_service.OperationMode;
import cz.it4i.fiji.datastore.security.Authorization;
import lombok.extern.log4j.Log4j2;
import mpicbg.spim.data.SpimDataException;

@Authorization
@ApplicationScoped

@Log4j2
@Path("/")
public class SharedDatasetServerEndpoint implements Serializable {

	private static final long serialVersionUID = 3030620649903413986L;

	@Inject
	BlockRequestHandler requestHandler;

	@Inject
	ApplicationConfiguration configuration;

	@Authorization
//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}")
	// @formatter:on
	@GET
	public Response getStatus(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version)
	{

		RootResponse result = RootResponse.builder().uuid(uuid).mode(
			OperationMode.READ_WRITE).version(Version.stringToIntVersion(version))
			.resolutionLevels(Collections.singletonList(
				new int[] { rX, rY, rZ })).build();
		ResponseBuilder responseBuilder = Response.ok();
		responseBuilder.entity(result).type(MediaType.APPLICATION_JSON_TYPE)
			.build();

		return responseBuilder.build();

	}

	@Authorization
//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/{" + X_PARAM + "}"
			+ "/{" + Y_PARAM + "}"
			+ "/{" + Z_PARAM + "}"
			+ "/{" + TIME_PARAM + "}"
			+ "/{" + CHANNEL_PARAM + "}"
			+ "/{" + ANGLE_PARAM +		"}"
			+ "{" + BLOCKS_PARAM + ":/?.*}")
	// @formatter:on
	@GET
	public Response readBlock(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(X_PARAM) long x, @PathParam(Y_PARAM) long y,
		@PathParam(Z_PARAM) long z, @PathParam(TIME_PARAM) int time,
		@PathParam(CHANNEL_PARAM) int channel, @PathParam(ANGLE_PARAM) int angle,
		@PathParam(BLOCKS_PARAM) String blocks)
	{

		return requestHandler.readBlock(getDataSetserver(uuid, rX, rY, rZ, version),
			x, y, z, time, channel, angle, blocks);

	}

	@Authorization
	// @formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/{" + X_PARAM + "}"
			+"/{" + Y_PARAM + "}"
			+"/{" +	Z_PARAM + "}"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}"
			+ "{" + BLOCKS_PARAM + ":/?.*}")
	// @formatter:on
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response writeBlock(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(X_PARAM) long x, @PathParam(Y_PARAM) long y,
		@PathParam(Z_PARAM) long z, @PathParam(TIME_PARAM) int time,
		@PathParam(CHANNEL_PARAM) int channel, @PathParam(ANGLE_PARAM) int angle,
		@PathParam(BLOCKS_PARAM) String blocks, InputStream inputStream)
	{
		return requestHandler.writeBlock(getDataSetserver(uuid, rX, rY, rZ,
			version), x, y, z, time, channel, angle, blocks, inputStream);
	}

	@Authorization
//@formatter:off
	@Path("datasets"
		  + "/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/datatype"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}")
	// @formatter:on
	@GET
	public Response getType(@PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) String version,
		@PathParam(TIME_PARAM) int time, @PathParam(CHANNEL_PARAM) int channel,
		@PathParam(ANGLE_PARAM) int angle)
	{
		return requestHandler.getType(getDataSetserver(uuid, rX, rY, rZ, version),
			time, channel, angle);
	}

	@POST
	@Path("datasets" + "/{" + UUID + "}" + "/{" + R_X_PARAM + "}" + "/{" +
		R_Y_PARAM + "}" + "/{" + R_Z_PARAM + "}" + "/{" + VERSION_PARAM + "}" +
		"/stop")
	public Response stopDataServer() {
		log.debug("Stop was requested as REST request and ignored");

		return Response.ok().build();
	}

	private DatasetServerImpl getDataSetserver(String uuid, int rX, int rY,
		int rZ, String version)
	{
		try {
			final boolean mixedVersion = Version.MIXED_LATEST_VERSION_NAME.equals(
				version);
			DatasetHandler handler = configuration.getDatasetHandler(uuid);
			int versionInt = mixedVersion?handler.getLatestVersion():stringToIntVersion(version);
			OperationMode mode = mixedVersion ? READ : READ_WRITE;
			return new DatasetServerImpl(handler, singletonList(new int[] { rX, rY,
				rZ }), versionInt, mixedVersion, mode);
		}
		catch (SpimDataException | IOException exc) {
			log.error("getDatasetServer", exc);
			throw new InternalServerErrorException(exc);
		}
	}

}
