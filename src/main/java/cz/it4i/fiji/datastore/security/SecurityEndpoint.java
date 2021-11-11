/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import static java.lang.String.format;

import io.vertx.core.json.JsonObject;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

@Path("/oauth2")
public class SecurityEndpoint {

	private static final String OAUTH_SERVER = "OAUTH_SERVER";

	private static final String CODE = "code";

	@Inject
	SecurityRegistry registry;

	@GET()
	@Path("{" + OAUTH_SERVER + "}")
	public Response get(@Context UriInfo request,
		@PathParam(OAUTH_SERVER) String oauthServer, @QueryParam(CODE) String code)
	{

		//@formatter:off
		return registry.findServer(oauthServer)
				    .map(s -> processRequest(request, s, code))
				    .orElseGet(() -> oauthServerNotExists(oauthServer));
		//@formatter:on

	}

	private Response processRequest(UriInfo request, OAuthServer s, String code) {
		if (code == null || code.isBlank()) {
			return redirectToOAuthServerLogin(request, s);
		}
		UserTokens tokens = s.getuUserAccessToken(code, request.getAbsolutePath());
		if (tokens == null) {
			return Response.status(Status.BAD_REQUEST).entity(
				"OAuth server request error").build();
		}
		UserInfo userInfo = s.getUserInfo(tokens.getAccessToken());
		if (userInfo == null) {
			return Response.status(Status.BAD_REQUEST).entity(
				"OAuth server request error").build();
		}

		JsonObject jsonObj = new JsonObject();
		jsonObj.put("userID", userInfo.getUserId());
		jsonObj.put("accessToken", tokens.getAccessToken());
		jsonObj.put("refreshToken", tokens.getRefreshToken());
		
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(jsonObj
			.toString()).build();
	}

	private static Response redirectToOAuthServerLogin(UriInfo request,
		OAuthServer server)
	{
		return Response.temporaryRedirect(URI.create(format(
			"%s?client_id=%s&redirect_uri=%s&response_type=code&scope=openid", server
				.getAuthURI(), server.getClientID(), request.getAbsolutePath()
					.toString())))
			.build();
	}

	private static Response oauthServerNotExists(String name) {
		return Response.status(Status.NOT_FOUND).entity(format(
			"OAuth server with name=\"%s\" is not registered", name)).build();
	}
}
