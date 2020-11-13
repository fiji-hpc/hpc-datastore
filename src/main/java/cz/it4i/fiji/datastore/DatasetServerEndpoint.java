/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.R_X_PARAM;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.R_Y_PARAM;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.R_Z_PARAM;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.UUID;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.VERSION_PARAM;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.X_PARAM;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.Y_PARAM;
import static cz.it4i.fiji.datastore.DatasetRegisterServiceEndpoint.Z_PARAM;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.internal.routing.RoutingContext;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("datasetserver")
public class DatasetServerEndpoint {

	private static final String TIME_PARAM = "TIME";

	private static final String CHANNEL_PARAM = "CHANNEL";

	private static final String ANGLE_PARAM = "ANGLE";
	private static final String BLOCKS_PARAM = "BLOCKS";

	@Inject
	private DatasetServerImpl datasetServer;

	// @formatter:off
	@Path("{" + UUID + "}"
			+"/{" + R_X_PARAM + "}"
			+"/{" + R_Y_PARAM + "}"
			+"/{" + R_Z_PARAM +	"}"
			+"/{" + VERSION_PARAM + "}"
			+"/{" + X_PARAM + "}"
			+"/{" + Y_PARAM + "}"
			+"/{" +	Z_PARAM + "}"
			+"/{" + TIME_PARAM + "}"
			+"/{" + CHANNEL_PARAM + "}"
			+"/{" + ANGLE_PARAM +		"}")
	// @formatter:on
	@POST
	public void writeBlock(RoutingContext ctx, @PathParam(UUID) String uuid,
		@PathParam(R_X_PARAM) int rX, @PathParam(R_Y_PARAM) int rY,
		@PathParam(R_Z_PARAM) int rZ, @PathParam(VERSION_PARAM) int version,
		@PathParam(X_PARAM) long x, @PathParam(Y_PARAM) long y,
		@PathParam(Z_PARAM) long z, @PathParam(TIME_PARAM) long time,
		@PathParam(CHANNEL_PARAM) int chanel, @PathParam(ANGLE_PARAM) int angle)
	{

	}

	// @formatter:off
	@Path("/{" + UUID + "}"
			+ "/{" + R_X_PARAM + "}"
			+ "/{" + R_Y_PARAM + "}"
			+ "/{" + R_Z_PARAM +	"}"
			+ "/{" + VERSION_PARAM + "}"
			+ "/{" + X_PARAM + "}"
			+ "/{" + Y_PARAM + "}"
			+ "/{" +	Z_PARAM + "}"
			+ "/{" + TIME_PARAM + "}"
			+ "/{" + CHANNEL_PARAM + "}"
			+ "/{" + ANGLE_PARAM +		"}"
			+ "/{" + BLOCKS_PARAM + ":.*}")
	// @formatter:on
	@GET
	public Response readBlock(ContainerRequestContext request,
		@PathParam(UUID) String uuid, @PathParam(R_X_PARAM) int rX,
		@PathParam(R_Y_PARAM) int rY, @PathParam(R_Z_PARAM) int rZ,
		@PathParam(VERSION_PARAM) String version, @PathParam(X_PARAM) long x,
		@PathParam(Y_PARAM) long y, @PathParam(Z_PARAM) long z,
		@PathParam(TIME_PARAM) int time, @PathParam(CHANNEL_PARAM) int channel,
		@PathParam(ANGLE_PARAM) int angle, @PathParam(BLOCKS_PARAM) String blocks, @MatrixParam("blocks") List<String> blocksList)
	{
		try {
			List<BlockIdentification> blocksId = new LinkedList<>();
			blocksId.add(new BlockIdentification(new long[] { x, y, z }, time,
				channel, angle));
			extract(blocks, blocksId);
			extract(blocksList, time, channel, angle, blocksId);
			List<BlockIdentification> notExistentBlocks = new LinkedList<>();
			ByteBuffer result = null;
			for (BlockIdentification bi : blocksId) {
				ByteBuffer block = datasetServer.read(new long[] { x, y, z }, time,
					channel, angle, new int[] { rX, rY, rZ });

				if (block == null) {
					notExistentBlocks.add(bi);
				}
				if (notExistentBlocks.isEmpty()) {
					if (result == null && block != null) {
						result = ByteBuffer.allocate(block.capacity() * blocksId
							.size());
					}
					if (result != null) {
						result.put(block);
					}
				}
			}
			if (notExistentBlocks.isEmpty() && result != null) {
				return Response.ok(result.array()).type(
					MediaType.APPLICATION_OCTET_STREAM).build();
			}
			return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
				.entity("Blocks [" + String.join(",", notExistentBlocks.stream().map(
					Object::toString).collect(Collectors.toList())) +
					"] not found on resolution level {rX:" + rX + ", rY:" + rY + ", rZ:" +
					rZ + "}.").build();
		}
		catch (IOException | NullPointerException exc) {
			log.warn("read", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}

	}

	private void extract(List<String> blocksList,
		int time, int channel, int angle, List<BlockIdentification> blocksId)
	{
		// TODO Auto-generated method stub

	}

	private void extract(String blocks, List<BlockIdentification> blocksId) {
		// TODO Auto-generated method stub

	}

	@AllArgsConstructor
	private static class BlockIdentification {

		@Getter
		private final long[] gridPosition;

		@Getter
		private final int time;

		@Getter
		private final int channel;

		@Getter
		private final int angle;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (long i : gridPosition) {
				sb.append(i).append("/");
			}
			sb.append(time).append("/").append(channel).append("/").append(angle);
			return sb.toString();
		}
	}
}