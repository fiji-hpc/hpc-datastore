/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Constants {

	static final String SECURITY_TOKEN = "cz.it4i.fiji.datastore.security.token";

	static final String SECURITY_USERS = "cz.it4i.fiji.datastore.security.users";

	static final String SECURITY_SERVERS =
		"cz.it4i.fiji.datastore.security.servers";

}
