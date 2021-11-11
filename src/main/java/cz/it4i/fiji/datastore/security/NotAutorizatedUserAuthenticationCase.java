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
class NotAutorizatedUserAuthenticationCase extends Authentication {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final private UserInfo userInfo;

	@Override
	public void processRequest(ContainerRequestContext requestContext) {
		requestContext.abortWith(Response.status(Status.FORBIDDEN).entity("User: " +
			userInfo.getName() + " is not authorized to access.").build());
	}
}
