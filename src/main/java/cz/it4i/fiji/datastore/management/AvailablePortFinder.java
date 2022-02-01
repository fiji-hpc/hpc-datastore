/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.management;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AvailablePortFinder {

	private static String PORTS_RANGE = "datastore.ports";

	private final int[] range;

	public AvailablePortFinder() {
		range = Optional.ofNullable(System.getProperty(PORTS_RANGE)).map(
			t -> createRange(t)).orElse(null);
	}

	private int[] createRange(String param) {
		String[] tokens = param.split("[,;]");
		if (tokens.length == 1) {
			return new int[] { Integer.parseInt(tokens[0]), 65553 };
		}

		return new int[] { Integer.parseInt(tokens[0]), Integer.parseInt(
			tokens[0]) };
	}
	
	
	public int findAvailablePort(String hostName)
		{

			OptionalInt result = OptionalInt.empty();
			Random rnd = new Random();
			do {
				int port = range == null ? 0 : rnd.nextInt(range[1] - range[0] + 1) +
					range[0];
				result = tryPort(hostName, port);
			} while (result.isEmpty());
			return result.getAsInt();
		}

		private OptionalInt tryPort(String hostName, int port) {

			try (ServerSocket socket = new ServerSocket(port, 1, InetAddress
				.getByName(hostName)))
			{
				return OptionalInt.of(socket.getLocalPort());
			}
			catch (IOException exc) {
				return OptionalInt.empty();
			}

		}

}
