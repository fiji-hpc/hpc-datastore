/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.s3;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class S3Settings {

	@Builder.Default
	private final String basePath = "";

	private final String hostURL;

	@Builder.Default
	private final String region = "eu-central-1";

	private final String accessKey;

	private final String secretKey;

	private final String bucket;
}
