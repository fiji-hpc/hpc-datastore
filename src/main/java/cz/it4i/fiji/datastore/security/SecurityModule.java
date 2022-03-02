/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import javax.ws.rs.container.ContainerRequestContext;

@Default
@RequestScoped
public class SecurityModule {

	@Inject
	SecurityRegistry securityRegistry;

	private Authentication authentication;

	public void processToken(BearerToken token)
	{
		authentication = securityRegistry.findAuthentication(token);

		if (authentication == null) {
			authentication = findAuthenticationByToken(token);
		}
	}

	public String getDataserverPropertyProperty() {
		return Optional.ofNullable(authentication).map(
			Authentication::getDataserverPropertyProperty).orElse(null);
	}

	public String getUserID() {
		return ofNullable(authentication).map(Authentication::getUserID)
			.map(Object::toString).orElse("None");
	}

	public void processRequest(ContainerRequestContext requestContext) {
		authentication.processRequest(requestContext);
	}

	public void checkAuthorization(InvocationContext ctx) {
		if (authentication != null) {
			authentication.checkAuthorization(ctx);
		}
	}

	private Authentication findAuthenticationByToken(BearerToken token)
	{
		if (token == BearerToken.NON_PRESENTED_TOKEN) {
			return new AnonymousUserAuthenticationCase();
		}
		for (OAuthServer server : securityRegistry.getServers()) {
			UserInfo userInfo = server.getUserInfo(token.getAccessToken());
			if (userInfo == null) {
				continue;
			}
			Authentication auth = securityRegistry.findAuthentication(userInfo
				.getUserId(), server);
			if (auth != null) {
				auth.setAccessToken(token.getAccessToken());
				securityRegistry.updateAuthentication(auth);
				return auth;
			} 
			return new NotAutorizatedUserAuthenticationCase(userInfo);
	
		}
		return new InvalidTokenAuthenticationCase(token.getAccessToken());
	}

}
