/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static cz.it4i.fiji.datastore.ApplicationConfiguration.BASE_NAME;

import java.nio.file.Path;

public final class DatasetPathRoutines {

	private DatasetPathRoutines() {}

	public static Path getDatasetVersionDirectory(Path baseDirectory,
		int version)
	{
		return baseDirectory.resolve("" + version);
	}

	public static Path getDataDirectory(Path datasetVersionDirectory)
	{
		return datasetVersionDirectory.resolve(BASE_NAME + ".n5");
	}

	public static Path getXMLFile(Path datasetVersionDirectory)
	{
		return datasetVersionDirectory.resolve(BASE_NAME + ".xml");
	}


}
