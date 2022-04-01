/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.management;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import lombok.extern.log4j.Log4j2;

@Log4j2
@ApplicationScoped
public class AvailablePortFinder {

	private static final int PROOFS_THRESHOLDS = 10;

	private static String PORTS_RANGE = "datastore.ports";

	private final int[] range;

	public AvailablePortFinder() {
		range = Optional.ofNullable(System.getProperty(PORTS_RANGE)).map(
			t -> createRange(t)).orElse(null);
	}

	private int[] createRange(String param) {
		try {
			String[] tokens = param.split("[,;]");
			if (tokens.length == 1) {
				return new int[] { Integer.parseInt(tokens[0]), 65535 };
			}

			return new int[] { Integer.parseInt(tokens[0]), Integer.parseInt(
				tokens[1]) };
		}
		catch (NumberFormatException exc) {
			log.warn("Illegal value '{}' for property '{}'. Used '0,65535' instead.",
				param, PORTS_RANGE);
			return null;
		}
	}
	
	
	public int findAvailablePort(String hostName)
		{

			OptionalInt result = OptionalInt.empty();
			Random rnd = new Random();
			int proofs = 0;
			do {
				int port = range == null ? 0 : rnd.nextInt(range[1] - range[0] + 1) +
					range[0];
				result = tryPort(hostName, port);
				proofs++;
				if (proofs > PROOFS_THRESHOLDS) {
					throw new UncheckedIOException(new IOException(
						"Cannot allocate port in range[" + range[0] + "," + range[1] +
							"]"));
				}
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
