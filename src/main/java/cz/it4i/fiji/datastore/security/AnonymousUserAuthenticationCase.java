/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import javax.interceptor.InvocationContext;
import javax.ws.rs.container.ContainerRequestContext;

class AnonymousUserAuthenticationCase extends Authentication {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void processRequest(ContainerRequestContext requestContext) {}

	@Override
	public void checkAuthorization(InvocationContext ctx) {
		throw new UnauthenticatedAccessException();
	}

	@Override
	public String getUserID() {
		return "Anonymous";
	}
}
