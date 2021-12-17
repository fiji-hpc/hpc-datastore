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
import java.util.Collection;
import java.util.HashSet;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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
	public Response get(@Context UriInfo requestURI, @Context HttpHeaders headers,
		@PathParam(OAUTH_SERVER) String oauthServer, @QueryParam(CODE) String code)
	{
		//@formatter:off
		return registry.findServer(oauthServer)
				    .map(s -> processRequest(new HashSet<>( headers.getAcceptableMediaTypes()),requestURI, s, code))
				    .orElseGet(() -> oauthServerNotExists(oauthServer));
		//@formatter:on

	}

	private Response processRequest(Collection<MediaType> acceptedTypes,
		UriInfo request, OAuthServer s, String code)
	{
		if (code == null || code.isBlank()) {
			return redirectToOAuthServerLogin(request, s);
		}
		UserTokens tokens = s.getuUserAccessToken(code, fixURI(request
			.getAbsolutePath()));
		if (tokens == null) {
			return Response.status(Status.BAD_REQUEST).entity(
				"OAuth server request error").build();
		}
		UserInfo userInfo = s.getUserInfo(tokens.getAccessToken());
		if (userInfo == null) {
			return Response.status(Status.BAD_REQUEST).entity(
				"OAuth server request error").build();
		}

		ResponseBuilder responseBuilder = Response.ok();
		if (acceptedTypes.contains(MediaType.APPLICATION_JSON_TYPE)) {
			getResponseAsJSON(responseBuilder, userInfo, tokens);
		}
		else if (acceptedTypes.contains(MediaType.TEXT_HTML_TYPE)) {
			getResponseAsHTML(responseBuilder, userInfo, tokens);
		}
		
		return responseBuilder.build();
	}

	private void getResponseAsHTML(ResponseBuilder responseBuilder,
		UserInfo userInfo, UserTokens tokens)
	{
		StringBuilder sb = new StringBuilder();
		// @formatter:off
		sb.append(
			"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-gb\" lang=\"en-gb\" dir=\"ltr\">").append('\n')
			.append("<head>").append('\n')
			.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />").append('\n')
			.append("</head>").append('\n')
			.append("<body>").append('\n');
			
			copyToClipboardJS(sb);
			sb
			.append("<h1>Authentication info</h1>").append('\n')
			.append("<div>User ID: <input  type=\"text\" value=\"").append(userInfo.getUserId())
			.append("\" id=\"userID\" readonly>")
			.append("<button onclick=\"myFunction('userID')\">Copy text</button></div>").append('\n')
			.append("<div>Access token: <input size=\"70\" type=\"text\" value=\"").append(tokens.getAccessToken())
			.append("\" id=\"accessToken\" readonly>")
			.append("<button onclick=\"myFunction('accessToken')\">Copy text</button></div>").append('\n')
			.append("<div>Refresh token: <input size=\"70\" type=\"text\" value=\"").append(tokens.getRefreshToken())
			.append("\" id=\"refreshToken\" readonly>")
			.append("<button onclick=\"myFunction('refreshToken')\">Copy text</button></div>").append('\n')
			.append("</body>").append('\n');
		// @formatter:on
		responseBuilder.type(MediaType.TEXT_HTML_TYPE).entity(sb.toString());
	}

	private void copyToClipboardJS(StringBuilder sb) {
	// @formatter:off
		sb
		.append("<script>\n")
		.append("function myFunction(myInput) {\n")
		.append("  var copyText = document.getElementById(myInput);\n")
		.append("  copyText.select();\n")
		.append("  copyText.setSelectionRange(0, 99999); /* For mobile devices */\n")
		.append("  /* Copy the text inside the text field */\n")
		.append("  navigator.clipboard.writeText(copyText.value);\n")
		.append("  /* Alert the copied text */\n")
		.append("  alert(\"Copied the text: \" + copyText.value);\n")
		.append("}")
		.append("</script>\n");
	
	}

	private void getResponseAsJSON(ResponseBuilder responseBuilder,
		UserInfo userInfo, UserTokens tokens)
	{
		JsonObject jsonObj = new JsonObject();
		jsonObj.put("userID", userInfo.getUserId());
		jsonObj.put("accessToken", tokens.getAccessToken());
		jsonObj.put("refreshToken", tokens.getRefreshToken());
		responseBuilder.type(MediaType.APPLICATION_JSON).entity(jsonObj.toString());
	}

	private static Response redirectToOAuthServerLogin(UriInfo request,
		OAuthServer server)
	{
		URI redirectURI = request.getRequestUri();
		redirectURI = fixURI(redirectURI);
		return Response.temporaryRedirect(URI.create(format(
			"%s?client_id=%s&redirect_uri=%s&response_type=code&scope=openid", server
				.getAuthURI(), server.getClientID(), redirectURI.toString())))
			.build();
	}

	private static URI fixURI(URI uri) {
		
		if (uri.getScheme().equals("https")) {
			uri = URI.create(uri.toString().replaceFirst(uri.getHost(), System.getProperty("quarkus.http.host", uri.getHost())));
		}
		return uri;
	}

	private static Response oauthServerNotExists(String name) {
		return Response.status(Status.NOT_FOUND).entity(format(
			"OAuth server with name=\"%s\" is not registered", name)).build();
	}
}
