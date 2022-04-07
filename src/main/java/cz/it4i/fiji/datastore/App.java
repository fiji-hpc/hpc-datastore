/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import cz.it4i.fiji.datastore.management.DataServerManager;
import cz.it4i.fiji.datastore.timout_shutdown.TimeoutTimer;
import lombok.extern.log4j.Log4j2;

//TODO locking datasets for read/write - with defined timeout
//TODO Starting  remote dataservers - use registerservice for start
//TODO - set proper working directory for tests - erase it after test running
@QuarkusMain
@Log4j2
public class App implements QuarkusApplication {

	private final static String PROPERTY_UUID = "fiji.hpc.data_store.uuid";

	private static final String PROPERTY_DATA_STORE_TIMEOUT =
		"fiji.hpc.data_store.timeout";

	@Inject
	TimeoutTimer timer;

	@Inject
	DataServerManager manager;

	private static String uuid;

	private static String port;

	private static LocalDateTime start;


	public static void main(String[] args) {
		String serverID;
		if (getUUID() != null) {
			start = LocalDateTime.now();
			uuid = getUUID();
			port = System.getProperty("quarkus.http.port");
			long timeout = Long.parseLong(System.getProperty(
				PROPERTY_DATA_STORE_TIMEOUT, "-1"));
			serverID = Hashing.sha256().hashString(uuid, UTF_8).toString().substring(
				0, 8) + ":" + port;
			log.info(
				"Server(id = {}) on port = {} for dataset = {} started with timeout {}s",
				serverID, port, uuid, timeout / 1000.0);
		}
		else {
			serverID = "RegisterService";
		}
		System.setProperty("server_id", serverID);
		Quarkus.run(App.class, App::handleExit, args);
	}

	@Override
	public int run(String... args) throws Exception {

		Quarkus.waitForExit();
		return 0;
	}

	public static void handleExit(Integer status, Throwable t) {
		if (t != null) {
			log.error("Unhandled exception", t);
		}
		if (start != null) {
			log.info(
				"Server(id = {}) on port = {} for dataset = {} stopped after {}s",
				System.getProperty("server_id"), port, uuid, ChronoUnit.SECONDS.between(
					start, LocalDateTime
					.now()));
		}
		System.exit(status);
	}

	private static String getUUID() {
		if (uuid != null) {
			return uuid;
		}
		uuid = System.getProperty(PROPERTY_UUID, "");
		if (uuid.isEmpty()) {
			return null;
		}
		try {
			return uuid;
		}
		catch (IllegalArgumentException exc) {
			log.warn("uuid={} passed as property is not valid", uuid);
			return null;
		}
	}
}
