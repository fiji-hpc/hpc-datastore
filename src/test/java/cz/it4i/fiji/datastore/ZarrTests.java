package cz.it4i.fiji.datastore;

import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.params.CoreConnectionPNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static io.restassured.RestAssured.with;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest()
public class ZarrTests {
    private String uuid;

    @BeforeEach
    void initUUID() {
        if (uuid != null) {
            return;
        }
        Response result = with().when().contentType("application/json").body(
                        " { \"voxelType\":\"uint32\", \"dimensions\":[1000,1000,1], \"timepoints\": 2, \"channels\": 2, \"angles\": 2, \"voxelUnit\": \"um\", \"voxelResolution\": [0.4, 0.4, 1], \"timepointResolution\": {\"value\": 1,\"unit\":\"min\"}, \"channelResolution\": {\"value\": 0,\"unit\":null}, \"angleResolution\": {\"value\":0 ,\"unit\":null}, \"compression\": \"raw\",\"datasetType\": \"Zarr\", \"resolutionLevels\": [ {\"resolutions\":[ 1, 1, 1],\"blockDimensions\":[ 64, 64, 64] }, {\"resolutions\":[ 2, 2, 1],\"blockDimensions\":[ 64, 64, 64]} ]}")
                .post("/datasets").andReturn();
        uuid = result.asString();
    }
    
    @Test
    public void isZarrCreated()
    {
        assertNotNull(uuid, "Dataset was not created");
        DatasetRepository datasetDAO = new DatasetRepository();
        String type = datasetDAO.findTypebyUUID(uuid);
        assertEquals("Zarr", type);

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
    private byte[] constructOneBlock(int dim) {
        return constructBlocks(1, dim);
    }
    private RequestSpecification withNoFollowRedirects() {
        return with().config(RestAssuredConfig.config().redirect(RedirectConfig
                .redirectConfig().followRedirects(false)));
    }
    @Test
    public void writeReadOneBlock() {
        Response result = withNoFollowRedirects().get("/datasets/" + uuid +
                "/1/1/1/new/write?timeout=" + 10000l);
        assertEquals(307, result.getStatusCode(), "Should be redirected but was " +
                result.asString());

        String redirectedURI = result.getHeader("Location");

        byte[] data = constructOneBlock(64);

        with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
                .post("/0/0/0/0/0/0");
        with().baseUri(redirectedURI).post("/stop");

        result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
                .redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
                "/1/1/1/latest/read?timeout=" + 10000l);
        assertEquals(307, result.getStatusCode(), "Should be redirected");
        redirectedURI = result.getHeader("Location");
        result = with().baseUri(redirectedURI).contentType(	ContentType.BINARY).get("/0/0/0/0/0/0");
        assertEquals(ContentType.BINARY.toString(), result.contentType());
        byte[] outputData = result.getBody().asByteArray();
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
                    "/1/1/1/new/write?timeout=" + 10000l);
            assertEquals(307, result.getStatusCode(), "Should be redirected");

            String redirectedURI = result.getHeader("Location");
            with().baseUri(redirectedURI).post("/stop");
        }
        with().config(config).body("10").post("/datasets/" + uuid +
                "/channels");
    }
}
