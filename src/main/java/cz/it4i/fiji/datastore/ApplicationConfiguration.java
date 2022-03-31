/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import cz.it4i.fiji.datastore.s3.DatasetS3Handler;
import cz.it4i.fiji.datastore.s3.S3Settings;

@ApplicationScoped
public class ApplicationConfiguration implements Serializable{

	private static final long serialVersionUID = -5159325588360781467L;

	private static final String DATASTORE_PATH = "datastore.path";

	private static final String DATASTORE_S3_HOST_URL = "datastore.s3.hostURL";

	private static final String DATASTORE_S3_REGION = "datastore.s3.region";

	private static final String DATASTORE_S3_BUCKET = "datastore.s3.bucket";

	private static final String DATASTORE_S3_ACCESS_KEY =
		"datastore.s3.accessKey";

	private static final String DATASTORE_S3_SECRET_KEY =
		"datastore.s3.secretKey";

	private static final String DATASTORE_S3_SECRET_KEY_VAR =
		"DATATASTORE_S3_SECRET_KEY";

	public static final String DEFAULT_PATH_PREFIX = "target/output";

	public static final Set<String> _properties = Arrays.asList(DATASTORE_PATH,
		DATASTORE_S3_HOST_URL, DATASTORE_S3_BUCKET, DATASTORE_S3_REGION,
		DATASTORE_S3_ACCESS_KEY, DATASTORE_S3_SECRET_KEY).stream().collect(toSet());

	public static final String BASE_NAME = "export";

	public DatasetHandler getDatasetHandler(String uuid) {
		String s3HostUrl = getProperty(DATASTORE_S3_HOST_URL);
		if (s3HostUrl != null) {
			return constructDataS3Handler(uuid, s3HostUrl);
		}
		return new DatasetFilesystemHandler(uuid, getDatasetPath(uuid));
	}

	public Path getDatastorePath() {
		return Paths.get(getProperty(DATASTORE_PATH, DEFAULT_PATH_PREFIX));
	}

	public Map<String,String> getConfiguredProperties() {
		Map<String, String> result = new HashMap<>();
		for (Entry<?, ?> entry : System.getProperties().entrySet()) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			if (_properties.contains(key)) {
				result.put(key, value);
			}
		}

		return result;
	}

	Path getDatasetPath(String uuid) {
		return getDatastorePath().resolve(uuid.toString());
	}

	private DatasetHandler constructDataS3Handler(String uuid, String s3HostUrl) {
		String pathPrefix = getProperty(DATASTORE_PATH, "");
		S3Settings.S3SettingsBuilder result = S3Settings.builder();
		result.hostURL(s3HostUrl).bucket(getProperty(DATASTORE_S3_BUCKET))
			.accessKey(getProperty(DATASTORE_S3_ACCESS_KEY)).secretKey(
				getSecretKey());
		String region = getProperty(DATASTORE_S3_REGION);
		if (region != null) {
			result.region(region);
		}
		return new DatasetS3Handler(uuid, pathPrefix, result.build());
	}

	private String getSecretKey() {
		String result = System.getenv(DATASTORE_S3_SECRET_KEY_VAR);
		if (result == null) {
			result = System.getProperty(DATASTORE_S3_SECRET_KEY);
		}
		return result;
	}

}
