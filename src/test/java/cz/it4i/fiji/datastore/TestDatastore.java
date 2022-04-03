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
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.nio.ByteBuffer;
import java.util.Random;

import javax.ws.rs.core.Response.Status;

import org.apache.http.params.CoreConnectionPNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * @author Jan Kožusznik
 */
@SuppressWarnings("deprecation")
@QuarkusTest()
@TestInstance(Lifecycle.PER_CLASS)
public class TestDatastore {

	final private long TIMEOUT = 10000l;
	private String uuid;

	@BeforeEach
	void initUUID() {
		if (uuid != null) {
			return;
		}
		Response result = with().when().contentType("application/json").body(
			" { \"voxelType\":\"uint32\", \"dimensions\":[1000,1000,1], \"timepoints\": 2, \"channels\": 2, \"angles\": 2, \"voxelUnit\": \"um\", \"voxelResolution\": [0.4, 0.4, 1], \"timepointResolution\": {\"value\": 1,\"unit\":\"min\"}, \"channelResolution\": {\"value\": 0,\"unit\":null}, \"angleResolution\": {\"value\":0 ,\"unit\":null}, \"compression\": \"raw\", \"resolutionLevels\": [ {\"resolutions\":[ 1, 1, 1],\"blockDimensions\":[ 64, 64, 64] }, {\"resolutions\":[ 2, 2, 1],\"blockDimensions\":[ 64, 64, 64]} ]}")
			.post("/datasets").andReturn();
		uuid = result.asString();
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
		
		byte[] data = constructOneBlock(64);

		with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
			.post("/0/0/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");

		result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
				"/1/1/1/latest/read?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		result = with().baseUri(redirectedURI).contentType(	ContentType.BINARY).get("/0/0/0/0/0/0");
		assertEquals(ContentType.BINARY.toString(), result.contentType());
		byte[] outputData = result.getBody().asByteArray();
		assertArrayEquals(data, outputData, "Result was: " + result.asString());
		with().baseUri(redirectedURI).post("/stop");
	}

	@Test
	public void writeReadTwoBlocks() {
		Response result = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new/write?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");

		String redirectedURI = result.getHeader("Location");

		byte[] data = constructBlocks(2, 64);

		with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
			.post("/0/0/0/0/0/0/0/1/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");

		result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
				"/1/1/1/latest/read?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		result = with().baseUri(redirectedURI).contentType(ContentType.BINARY).get(
			"/0/0/0/0/0/0/0/1/0/0/0/0");
		assertEquals(ContentType.BINARY.toString(), result.contentType(),
			"expected binary but obtained: " + result.body().asString());
		byte[] outputData = result.getBody().asByteArray();
		assertArrayEquals(data, outputData);
		with().baseUri(redirectedURI).post("/stop");
	}

	@Test
	public void mixedLatest() {
		String baseURI = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new/write?timeout=" + TIMEOUT).getHeader("Location");
		byte[] block1 = constructBlocks(1, 64);
		with().baseUri(baseURI).contentType(ContentType.BINARY).body(block1).post(
			"/0/0/0/0/0/0");
		with().baseUri(baseURI).post("/stop");

		baseURI = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new/write?timeout" + TIMEOUT).getHeader("Location");
		byte[] block2 = constructBlocks(1, 64);

		with().baseUri(baseURI).contentType(ContentType.BINARY).body(block2).post(
			"/0/1/0/0/0/0");
		with().baseUri(baseURI).post("/stop");

		baseURI = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/mixedLatest/read?timeout=" + TIMEOUT).getHeader(
				"Location");

		byte[] sentData = new byte[block1.length + block2.length];
		System.arraycopy(block1, 0, sentData, 0, block1.length);
		System.arraycopy(block2, 0, sentData, block1.length, block2.length);

		Response result = with().baseUri(baseURI).contentType(ContentType.BINARY)
			.get("/0/0/0/0/0/0/0/1/0/0/0/0");
		assertEquals(ContentType.BINARY.toString(), result.contentType(),
			"expected binary but obtained: " + result.body().asString());
		byte[] outputData = result.getBody().asByteArray();
		assertArrayEquals(sentData, outputData);
		with().baseUri(baseURI).post("/stop");

	}

	@Test
	public void setGetMetadata() {
		String testMetadata = "test metadata" + Math.random();
		assertEquals(Status.OK.getStatusCode(), with().contentType(ContentType.TEXT)
			.body(testMetadata).post("/datasets/" + uuid + "/common-metadata")
			.getStatusCode());
		String readedMatadata = with().get("/datasets/" + uuid + "/common-metadata")
			.getBody().asString();
		assertEquals(testMetadata, readedMatadata);
	}

	@Test
	public void readNonExistingBlock() {
		Response result = with().config(RestAssuredConfig.config().redirect(
			RedirectConfig.redirectConfig().followRedirects(false))).get(
				"/datasets/" + uuid + "/1/1/1/latest/read?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		String redirectedURI = result.getHeader("Location");
		result = with().baseUri(redirectedURI).contentType(ContentType.BINARY).get(
			"/10/10/10/0/0/0");
		assertEquals(ContentType.BINARY.toString(), result.contentType());
		byte[] outputData = result.getBody().asByteArray();
		ByteBuffer bb = ByteBuffer.allocate(12);
		bb.putInt(-1);
		bb.putInt(-1);
		bb.putInt(-1);
		byte[] data = bb.array();
		assertArrayEquals(data, outputData, "Result was: " + result.asString());
		with().baseUri(redirectedURI).post("/stop");
	}

	@Test
	/**
	 * Test reading blocks - existing, non-existing, existing
	 */
	public void readE_NE_E_Block() {
		// create two blocks
		Response result = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new/write?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");

		String redirectedURI = result.getHeader("Location");

		byte[] data = constructBlocks(2, 64);

		with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
			.post("/0/0/0/0/0/0/0/1/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");

		result = with().config(RestAssuredConfig.config().redirect(
			RedirectConfig.redirectConfig().followRedirects(false))).get(
				"/datasets/" + uuid + "/1/1/1/latest/read?timeout=" + TIMEOUT);
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		result = with().baseUri(redirectedURI).contentType(ContentType.BINARY).get(
			"/0/0/0/0/0/0/10/10/10/0/0/0/0/1/0/0/0/0");
		assertEquals(ContentType.BINARY.toString(), result.contentType());
		byte[] outputData = result.getBody().asByteArray();
		ByteBuffer bb = ByteBuffer.allocate(12);
		bb.putInt(-1);
		bb.putInt(-1);
		bb.putInt(-1);
		byte[] nonExistingData = bb.array();
		bb = ByteBuffer.allocate(data.length + nonExistingData.length);
		bb.put(data, 0, data.length / 2);
		bb.put(nonExistingData);
		bb.put(data, data.length / 2, data.length / 2);
		data = bb.array();
		assertArrayEquals(data, outputData, "Result was: " + result.asString());
		with().baseUri(redirectedURI).post("/stop");
	}

	@Test
	public void addChannels() {
		RestAssuredConfig config = RestAssured.config().httpClient(HttpClientConfig
			.httpClientConfig().setParam(CoreConnectionPNames.CONNECTION_TIMEOUT,
				1000).setParam(CoreConnectionPNames.SO_TIMEOUT, 600000));

		for (int i = 0; i < 2; i++) {
			Response result = withNoFollowRedirects().get("/datasets/" + uuid +
				"/1/1/1/new/write?timeout=" + TIMEOUT);
			assertEquals(307, result.getStatusCode(), "Should be redirected");

			String redirectedURI = result.getHeader("Location");
			with().baseUri(redirectedURI).post("/stop");
		}
		with().config(config).body("10").post("/datasets/" + uuid +
			"/channels");
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
