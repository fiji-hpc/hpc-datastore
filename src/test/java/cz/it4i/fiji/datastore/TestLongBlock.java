/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static io.restassured.RestAssured.with;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.runtime.Quarkus;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import lombok.extern.log4j.Log4j2;

/**
 * @author Jan Kožusznik
 */
@Log4j2
@QuarkusTest()
@TestInstance(Lifecycle.PER_CLASS)
public class TestLongBlock {

	final private long TIMEOUT = 100000l;
	private String uuid;

	@BeforeEach
	void initUUID() {
		if (uuid != null) {
			return;
		}
		Response result = with().when().contentType("application/json").body(
			" {\"label\":\"Unit test\",  \"voxelType\":\"uint32\", \"dimensions\":[100000,100000,100000], \"timepoints\": 2, \"channels\": 2, \"angles\": 2, \"voxelUnit\": \"um\", \"voxelResolution\": [0.4, 0.4, 1], \"timepointResolution\": {\"value\": 1,\"unit\":\"min\"}, \"channelResolution\": {\"value\": 0,\"unit\":null}, \"angleResolution\": {\"value\":0 ,\"unit\":null}, \"compression\": \"raw\", \"resolutionLevels\": [ {\"resolutions\":[ 1, 1, 1],\"blockDimensions\":[ 256, 256, 256] }, {\"resolutions\":[ 2, 2, 1],\"blockDimensions\":[ 64, 64, 64]} ]}")
			.post("/datasets").andReturn();
		uuid = result.asString();
		log.info("status {}", result.getStatusLine());
	}

	void shutdown() {
		Quarkus.asyncExit();
	}

	@Test
	public void createDataset() {
		assertNotNull(uuid, "Dataset was not created");
	}

	@Test
	public void writeReadOneBlock() {
		Response result = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new/write?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected but was " +
			result.asString());

		String redirectedURI = result.getHeader("Location");
		
		byte[] data = constructOneBlock(256);
		try {
			with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
				.post("/0/0/0/0/0/0");
		}
		finally {
			with().baseUri(redirectedURI).post("/stop");
		}
		result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
				"/1/1/1/latest/read?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		try {
			result = with().baseUri(redirectedURI).contentType(ContentType.BINARY)
				.get("/0/0/0/0/0/0");
			assertEquals(ContentType.BINARY.toString(), result.contentType());
			byte[] outputData = result.getBody().asByteArray();
			assertArrayEquals(data, outputData, "Result was: " + result.asString());
		}
		finally {
			with().baseUri(redirectedURI).post("/stop");
		}
	}

	@Test
	public void writeReadTwoBlocks() {
		Response result = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new/write?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");

		String redirectedURI = result.getHeader("Location");

		byte[] data = constructBlocks(2, 256);
		try {
			with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
				.post("/0/0/0/0/0/0/0/1/0/0/0/0");
		}
		finally {
			with().baseUri(redirectedURI).post("/stop");
		}

		result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
				"/1/1/1/latest/read?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		try {
			result = with().baseUri(redirectedURI).contentType(ContentType.BINARY)
				.get("/0/0/0/0/0/0/0/1/0/0/0/0");
			assertEquals(ContentType.BINARY.toString(), result.contentType(),
				"expected binary but obtained: " + result.body().asString());
			byte[] outputData = result.getBody().asByteArray();
			assertArrayEquals(data, outputData);
		}
		finally {
			with().baseUri(redirectedURI).post("/stop");
		}
	}


	private RequestSpecification withNoFollowRedirects() {
		return with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false)));
	}

	private byte[] constructOneBlock(int dim) {
		return constructBlocks(1, dim);
	}

	private byte[] constructBlocks(int num, int dim) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(dim);
		int sizeOfOneBlock = (dim * dim * dim + 3) * 4;
		byte[] data = new byte[sizeOfOneBlock * num];
		new Random().nextBytes(data);
		for (int i = 0; i < num; i++) {
			int offset = sizeOfOneBlock * i;
			bb.flip();
			bb.get(data, offset + 0, 4);
			bb.clear();
			bb.get(data, offset + 4, 4);
			bb.clear();
			bb.get(data, offset + 8, 4);
		}
		return data;
	}

}
