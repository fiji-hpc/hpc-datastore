/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.s3;

import static cz.it4i.fiji.datastore.ApplicationConfiguration.BASE_NAME;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class DatasetS3KeyRoutines {

	private final String delimiter;

	String getDatasetVersionKey(String baseDirectory, int version)
	{
		return resolve(baseDirectory, version + delimiter);
	}

	String getDataKey(String datasetVersionKey)
	{
		return resolve(datasetVersionKey, BASE_NAME + ".n5" + delimiter);
	}

	String getXMLKey(String datasetVersionKey)
	{
		return resolve(datasetVersionKey, BASE_NAME + ".xml");
	}

	String resolve(String base, String child) {
		StringBuilder sb = new StringBuilder(base);
		if (!base.isEmpty() && !base.endsWith(delimiter) && !child.isEmpty() &&
			!child.startsWith(delimiter))
		{
			sb.append(delimiter);
		}
		else if (base.endsWith(delimiter) && child.startsWith(delimiter)) {
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.append(child);
		return sb.toString();
	}

}
