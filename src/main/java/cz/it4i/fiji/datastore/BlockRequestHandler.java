/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Default
@ApplicationScoped
public class BlockRequestHandler {

	private static final Pattern URL_BLOCKS_PATTERN = Pattern.compile(
		"(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)/(\\p{Digit}+)");

	public Response readBlock(DatasetServerImpl datasetServer, long x, long y,
		long z, int time, int channel, int angle, String blocks)
	{
		try {
			List<BlockIdentification> blocksId = new LinkedList<>();
			blocksId.add(new BlockIdentification(new long[] { x, y, z }, time,
				channel, angle));
			BlockIdentification.extract(blocks, blocksId);
			DataType dataType = null;
			try (DataBlockInputStream result = new DataBlockInputStream()) {
				for (BlockIdentification bi : blocksId) {
					long[] position = new long[] { bi.gridPosition[0], bi.gridPosition[1],
						bi.gridPosition[2] };
					DataBlock<?> block = datasetServer.read(position, bi.time, bi.channel,
						bi.angle);
					// block do not exist - return empty block having size [-1, -1, -1]
					if (block == null) {
						if (dataType == null) {
							dataType = datasetServer.getType(time, channel, angle);
						}
						block = dataType.createDataBlock(new int[] { -1, -1, -1 }, position,
							0);
					}
					result.add(block);
				}
				return Response.ok(result).type(MediaType.APPLICATION_OCTET_STREAM)
					.build();
			}
		}
		catch (IOException | NullPointerException exc) {
			log.warn("read", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
	}


	public Response writeBlock(DatasetServerImpl datasetServer, long x, long y,
		long z, int time, int channel, int angle, String blocks,
		InputStream inputStream)
	{
		List<BlockIdentification> blocksId = new LinkedList<>();
		blocksId.add(new BlockIdentification(new long[] { x, y, z }, time, channel,
			angle));
		BlockIdentification.extract(blocks, blocksId);
		try {

			for (BlockIdentification blockId : blocksId) {
				datasetServer.write(blockId.gridPosition, blockId.time, blockId.channel,
					blockId.angle, inputStream);
			}
		}
		catch (IOException exc) {
			log.warn("write", exc);
			return Response.serverError().entity(exc.getMessage()).type(
				MediaType.TEXT_PLAIN).build();
		}
		return Response.ok().build();
	}

	public Response getType(DatasetServerImpl datasetServer, int time,
		int channel, int angle)
	{
		DataType dt = datasetServer.getType(time, channel, angle);
		if (dt != null) {
			return Response.ok(dt.toString()).build();
		}
		return Response.status(Status.NOT_FOUND).build();
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

		static void extract(String blocks,
			List<BlockIdentification> blocksId)
		{

			Matcher matcher = URL_BLOCKS_PATTERN.matcher(blocks);
			while (matcher.find()) {
				blocksId.add(new BlockIdentification(new long[] { getLong(matcher, 1),
					getLong(matcher, 2), getLong(matcher, 3) }, getInt(matcher, 4),
					getInt(matcher, 5), getInt(matcher, 6)));
			}
		}

		static int getInt(Matcher matcher, int i) {
			return Integer.parseInt(matcher.group(i));
		}

		static long getLong(Matcher matcher, int i) {
			return Long.parseLong(matcher.group(i));
		}
	}

}
