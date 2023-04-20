/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import static com.google.common.base.Strings.emptyToNull;
import static cz.it4i.fiji.datastore.security.Constants.*;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Default
@ApplicationScoped
class SecurityRegistry {

	private Map<String, OAuthServer> servers;

	private Boolean securityDisabled;
	private Boolean staticSecurity;


	public List<String> getServerAliases()
	{
		List<String> names=new ArrayList<>();
		for(int i=0;i<servers.size();i++)
		{
			names.add(servers.get(i).getName());
		}
		return names;
	}
	public void refreshServers()
	{
		if (isSecurityDisabled()) {
			return;
		}
		List<String[]> list_server= new ArrayList<>();
		LinkedList<OAuthServer> temp = new LinkedList<>();

		if(isStaticSercuritySet()) {
			String[] serversTokens = getProperty(SECURITY_SERVERS).split(";");
			list_server.add(serversTokens);
		}
		List<OAuthServerNew> list_all_servers=OAuthServerNew.listAll();

		for (int i=0;i<list_all_servers.size();i++){
			list_server.add(list_all_servers.get(i).BCSerlialize().split(";"));

		}
		//TODO depredicated
		for (int i=0;i<list_all_servers.size();i++){
			for (String server : list_server.get(i)) {

				String[] serverTokens = server.trim().split(",");

				if(serverTokens.length==5) {
					temp.add(OAuthServer.builder().name(serverTokens[0]).attributeIDName(
							serverTokens[1]).authURI(URI.create(serverTokens[2])).userInfoURI(URI
							.create(serverTokens[3])).tokenEndpointURI(URI.create(
							serverTokens[4])).clientID("None").clientSecret(
							"None").build());
				}
				else
				{
					temp.add(OAuthServer.builder().name(serverTokens[0]).attributeIDName(
							serverTokens[1]).authURI(URI.create(serverTokens[2])).userInfoURI(URI
							.create(serverTokens[3])).tokenEndpointURI(URI.create(
							serverTokens[4])).clientID(serverTokens[5]).clientSecret(
							serverTokens[6]).build());
				}

			}
		}

		servers = temp.stream().collect(Collectors.toMap(OAuthServer::getName,
				s -> s));
	}

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

	public boolean isStaticSercuritySet()
	{
		System.out.println("Security token:"+getProperty(SECURITY_TOKEN));
		System.out.println("Security token:"+getProperty(SECURITY_ABLED));
		System.out.println("Security users:"+getProperty(Constants.SECURITY_USERS));
		System.out.println("Security servers:"+getProperty(Constants.SECURITY_SERVERS));
		if(staticSecurity==null) {
			staticSecurity= propertyIsNotEmpty(Constants.SECURITY_TOKEN) &&
					propertyIsNotEmpty(SECURITY_USERS) || propertyIsNotEmpty(SECURITY_SERVERS);
			if (!staticSecurity) {
				log.warn("Security is disabled");

			}
		}
		return staticSecurity;
	}

	public boolean isSecurityDisabled() {
		if (securityDisabled == null) {
				if(getProperty(Constants.SECURITY_ABLED).equals("YES"))
				{
					securityDisabled=false;
				}
				else
				{
					securityDisabled=true;
				}
		}
		System.out.println("Security State:"+securityDisabled);
		return securityDisabled;

	}

	private static boolean propertyIsNotEmpty(String propertyName) {
		return emptyToNull(getProperty(propertyName)) != null;
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

	public void initServers() {
		if (isSecurityDisabled()) {
			return;
		}
		List<String[]> list_server= new ArrayList<>();
		LinkedList<OAuthServer> temp = new LinkedList<>();

		if(isStaticSercuritySet()) {
			String[] serversTokens = getProperty(SECURITY_SERVERS).split(";");
			list_server.add(serversTokens);
		}

		List<OAuthServerNew> list_all_servers;
		OAuthServerService oauthServerService=new OAuthServerService();

		list_all_servers=oauthServerService.getAllOAuthServers();

		for (int i=0;i<list_all_servers.size();i++){
			list_server.add(list_all_servers.get(i).BCSerlialize().split(";"));

		}

		//TODO depredicated
		for (int i=0;i<list_all_servers.size();i++){
			for (String server : list_server.get(i)) {
				String[] serverTokens = server.trim().split(",");
				temp.add(OAuthServer.builder().name(serverTokens[0]).attributeIDName(
					serverTokens[1]).authURI(URI.create(serverTokens[2])).userInfoURI(URI
						.create(serverTokens[3])).tokenEndpointURI(URI.create(
							serverTokens[4])).clientID(serverTokens[5]).clientSecret(
								serverTokens[6]).build());

			}
		}

		servers = temp.stream().collect(Collectors.toMap(OAuthServer::getName,
			s -> s));
	}

	public void initUsers() {
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
	public void refreshUsers() {
		if (isSecurityDisabled()) {
			return;
		}
		users = new LinkedList<>();
		List<OAuthUserNew> list_all_users;
		List<String[]> list_user = new ArrayList<>();
		if(isStaticSercuritySet()) {
			String[] usersProperty = ofNullable(getProperty(SECURITY_USERS)).map(
					prop -> prop.split("&")).orElse(new String[0]);
			list_user.add(usersProperty); //TODO depredicated
		}
		list_all_users = OAuthUserNew.listAll();

		for (int i = 0; i < list_all_users.size(); i++) {
			list_user.add(list_all_users.get(i).BCSerlialize_user().split(";"));

		}

		for (int i = 0; i < list_all_users.size(); i++) {
			for (String user : list_user.get(i)) {

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
				//TODO zde muže být problém
				Authentication temp = Authentication.builder().server(server).userID(
								userID).user(new User(internalID, Arrays.asList(builder.build())))
						.build();
				users.add(temp);
			}

		}


	}
	void updateAuthentication(
		@SuppressWarnings("unused") Authentication auth)
	{
		// do nothing as it is not persistent yet
	}

}
