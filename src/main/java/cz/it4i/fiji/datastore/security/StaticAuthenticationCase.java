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

import cz.it4i.fiji.datastore.DatasetServerEndpoint;
import cz.it4i.fiji.datastore.management.DataServerManagerEndpoint;
import lombok.extern.log4j.Log4j2;

@Log4j2
class StaticAuthenticationCase extends Authentication {

	StaticAuthenticationCase(String accessToken)
	{
		super(accessToken, null, null, null);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2208988404323396594L;

	@Override
	public void checkAuthorization(InvocationContext ctx) {
		if (!(ctx.getTarget() instanceof DatasetServerEndpoint || ctx
			.getTarget() instanceof DataServerManagerEndpoint))
		{
			log.info("ctx.target = {}", ctx.getTarget());
			throw new UnauthorizedAccessException(getAccessToken());
		}
	}

	@Override
	public void processRequest(ContainerRequestContext requestContext) {
		// do nothing
	}

}
