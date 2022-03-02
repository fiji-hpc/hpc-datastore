/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import static cz.it4i.fiji.datastore.security.BearerToken.NON_PRESENTED_TOKEN;

import com.google.common.base.Strings;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class OAuthSecurityFilter implements ContainerRequestFilter {

	@Inject
	SecurityRegistry securityRegistry;

	@Inject
	SecurityModule module;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException
	{
		if (securityRegistry.isSecurityDisabled()) {
			return;
		}
		BearerToken token = extractToken(requestContext);

		module.processToken(token);
		module.processRequest(requestContext);
	}

	private BearerToken extractToken(ContainerRequestContext requestContext) {

		String[] token = Strings.nullToEmpty(requestContext.getHeaderString(
			"Authorization")).split("\\s+");
		if (token.length < 2 || !token[0].equals("Bearer")) {
			return NON_PRESENTED_TOKEN;
		}
		return new BearerToken(token[1].stripTrailing());
	}

}
