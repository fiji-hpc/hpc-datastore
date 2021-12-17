/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import javax.persistence.Entity;

import cz.it4i.fiji.datastore.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
class OAuthServer extends BaseEntity {

	private static final long serialVersionUID = 1L;

	private URI authURI;

	private URI redirectURI;

	private URI userInfoURI;

	private URI tokenEndpointURI;

	private String clientID;

	private String clientSecret;

	private String name;

	private String attributeIDName;



	public cz.it4i.fiji.datastore.security.UserInfo getUserInfo(
		String accessToken)
	{
		if (accessToken.isBlank()) {
			return null;
		}
		HTTPResponse response;
		try {
			response = new UserInfoRequest(userInfoURI, new BearerAccessToken(
				accessToken)).toHTTPRequest().send();
			UserInfoResponse userInfoResponse = UserInfoResponse.parse(response);
			if (userInfoResponse instanceof UserInfoErrorResponse) {

				return null;
			}
			UserInfo userInfo = userInfoResponse.toSuccessResponse().getUserInfo();
			return cz.it4i.fiji.datastore.security.UserInfo.builder().userId(userInfo
				.getClaim(attributeIDName).toString()).name(userInfo.getName()).build();
		}
		catch (IOException | ParseException exc) {
			log.error("parser OAuth request", exc);
		}
		return null;
	}
	
	public UserTokens getuUserAccessToken(String code, URI callback) {
		// Construct the code grant from the code obtained from the authz endpoint
		// and the original callback URI used at the authz endpoint
		AuthorizationCode authorizationCode = new AuthorizationCode(code);
		AuthorizationGrant codeGrant = new AuthorizationCodeGrant(authorizationCode,
			callback);

		// The credentials to authenticate the client at the token endpoint
		ClientAuthentication clientAuth = new ClientSecretBasic(new ClientID(
			clientID), new Secret(clientSecret));

		// The token endpoint
		URI tokenEndpoint = tokenEndpointURI;

		// Make the token request
		TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth,
			codeGrant);

		TokenResponse response;
		try {
			response = TokenResponse.parse(request.toHTTPRequest().send());
		}
		catch (ParseException | IOException exc) {
			log.error("parser OAuth request", exc);
			return null;
		}

		if (!response.indicatesSuccess()) {
			return null;
		}

		AccessTokenResponse successResponse = response.toSuccessResponse();

		// Get the access token, the server may also return a refresh token
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		RefreshToken refreshToken = successResponse.getTokens().getRefreshToken();
		return new UserTokens(accessToken.getValue(), Optional.ofNullable(
			refreshToken).map(RefreshToken::getValue).orElse(""));
	}
	
	
}
