/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class InvalidTokenAuthenticationCase extends Authentication {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String token;

	@Override
	public void processRequest(ContainerRequestContext requestContext) {

		requestContext.abortWith(Response.status(Status.UNAUTHORIZED).entity(
			Status.UNAUTHORIZED.getStatusCode() + ": \"" + token +
				"\" is not a valid token.")
			.build());
	}
}
