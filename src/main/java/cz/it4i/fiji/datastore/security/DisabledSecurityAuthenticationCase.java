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

class DisabledSecurityAuthenticationCase extends Authentication {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2208988404323396594L;

	@Override
	public void checkAuthorization(InvocationContext ctx) {
		// do nothing
	}

	@Override
	public void processRequest(ContainerRequestContext requestContext) {
		// do nothing
	}

	@Override
	public String getDataserverPropertyProperty() {
		return null;
	}
}
