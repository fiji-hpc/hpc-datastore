/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import static com.google.common.base.Strings.emptyToNull;
import static cz.it4i.fiji.datastore.security.Constants.SECURITY_SERVERS;
import static cz.it4i.fiji.datastore.security.Constants.SECURITY_TOKEN;
import static cz.it4i.fiji.datastore.security.Constants.SECURITY_USERS;
import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Default
@ApplicationScoped
class SecurityRegistry {

	private Map<String, OAuthServer> servers;

	private Boolean securityDisabled;

	private Collection<Authentication> users;
	{
		initServers();
		initUsers();
	}

	Authentication findAuthentication(BearerToken token) {
		if (isSecurityDisabled()) {
			return new DisabledSecurityAuthenticationCase();
		}

		String exclusiveToken = System.getProperty(SECURITY_TOKEN);
		if (exclusiveToken != null) {
			if (!exclusiveToken.equals(token.getAccessToken())) {
				return new InvalidTokenAuthenticationCase(token.getAccessToken());
			}
			return new StaticAuthenticationCase(exclusiveToken);
		}


		return users.stream().filter(a -> token.getAccessToken().equals(a
			.getAccessToken())).findFirst().orElse(null);
	}

	public boolean isSecurityDisabled() {
		if (securityDisabled == null) {
			securityDisabled = propertyIsEmpty(Constants.SECURITY_TOKEN) &&
				propertyIsEmpty(SECURITY_USERS) || propertyIsEmpty(SECURITY_SERVERS);
			if (securityDisabled) {
				log.warn("Security is disabled");
			}
		}
		return securityDisabled;

	}

	private static boolean propertyIsEmpty(String propertyName) {
		return emptyToNull(getProperty(propertyName)) == null;
	}

	Collection<OAuthServer> getServers(){
		return servers.values();
	}

	Authentication findAuthentication(String userID, OAuthServer server) {
		return users.stream().filter(a -> a.getUserID().equals(userID) && a
			.getServer().equals(server)).findFirst().orElse(null);
	}

	public Optional<OAuthServer> findServer(String oauthServer) {
		return ofNullable(servers.get(oauthServer.toLowerCase()));
	}

	private void initServers() {
		if (isSecurityDisabled()) {
			return;
		}
		String[] serversTokens = getProperty(SECURITY_SERVERS).split(";");
		LinkedList<OAuthServer> temp = new LinkedList<>();

		for (String server : serversTokens) {
			String[] serverTokens = server.trim().split(",");
			temp.add(OAuthServer.builder().name(serverTokens[0]).attributeIDName(
				serverTokens[1]).authURI(URI.create(serverTokens[2])).userInfoURI(URI
					.create(serverTokens[3])).tokenEndpointURI(URI.create(
						serverTokens[4])).clientID(serverTokens[5]).clientSecret(
							serverTokens[6]).build());

		}


		servers = temp.stream().collect(Collectors.toMap(OAuthServer::getName,
			s -> s));
	}

	private void initUsers() {
		if (isSecurityDisabled()) {
			return;
		}

		users = new LinkedList<>();
		String[] usersProperty = ofNullable(getProperty(SECURITY_USERS)).map(
			prop -> prop.split("&")).orElse(new String[0]);

		for (String user : usersProperty) {
			String[] userTokens = user.split(":");
			if (userTokens.length < 3) {
				throw new IllegalArgumentException("Invalid specification for user: " +
					user);

			}
			OAuthServer server = servers.get(userTokens[0].toLowerCase());
			if (server == null) {
				throw new IllegalArgumentException("Unknown OAUthserver: " +
					userTokens[0] + " for user: " + user);
			}
			String userID = userTokens[1];
			
			Long internalID = Long.parseLong(userTokens[2]);
			ACL.ACLBuilder builder = ACL.builder();
			if (userTokens.length > 3) {
				builder.write(userTokens[3].toLowerCase().equals("write"));
			}
			Authentication temp = Authentication.builder().server(server).userID(
				userID).user(new User(internalID, Arrays.asList(builder.build())))
				.build();
			users.add(temp);
		}

	}

	void updateAuthentication(
		@SuppressWarnings("unused") Authentication auth)
	{
		// do nothing as it is not persistent yet
	}

}
