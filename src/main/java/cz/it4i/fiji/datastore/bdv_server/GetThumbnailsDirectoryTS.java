/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv_server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class GetThumbnailsDirectoryTS {

	public static String $() throws IOException {
		java.nio.file.Path temp = Paths.get(System.getProperty("java.io.tmpdir"))
			.resolve("hpc-datastore");
		if (!Files.exists(temp)) {
			Files.createDirectory(temp);
		}
		return temp.toString();
	}

}
