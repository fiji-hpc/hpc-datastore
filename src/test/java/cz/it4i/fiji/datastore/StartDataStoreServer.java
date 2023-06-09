package cz.it4i.fiji.datastore;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@QuarkusTest()
public class StartDataStoreServer {

	@Test
	public void testRunning() throws IOException {
		System.out.println("just close me from IDE when done...");
		new BufferedReader(new InputStreamReader(System.in)).readLine();
		//NB: makes it wait forever...
	}
}