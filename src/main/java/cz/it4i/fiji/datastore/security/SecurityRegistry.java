package cz.it4i.fiji.datastore.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.emptyToNull;
import static cz.it4i.fiji.datastore.security.Constants.*;
import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;

@Slf4j
@Default
@ApplicationScoped
public class SecurityRegistry {
    @Inject
    OAuthServerService oauthServerService;
    @Inject
    OAuthUserService oauthUserService;
    private Map<String, OAuthServer> servers;
    @Getter
    private Boolean securityDisabled;

    {
            if(getProperty(Constants.SECURITY_ON).equals("YES"))
            {
                securityDisabled=false;
            }
            else
            {
                securityDisabled=true;
            }
    };

    private Collection<Authentication> users;
    {
        initServers();
        initUsers();
    }

    //Servers
    Collection<OAuthServer> getServers(){
        List<OAuthServer> serversPersistent=oauthServerService.getAllOAuthServers();
        serversPersistent.addAll(servers.values());
        return serversPersistent;
    }
    public Optional<OAuthServer> findServer(String oauthServer) {
        List<OAuthServer> serversPersistent=oauthServerService.getAllOAuthServers();
        serversPersistent.addAll(servers.values());
        Map<String, OAuthServer> serversWithNames = serversPersistent.stream().collect(Collectors.toMap(OAuthServer::getName,
                s -> s));
        return ofNullable(serversWithNames.get(oauthServer.toLowerCase()));
    }

    private static boolean propertyIsNotEmpty(String propertyName) {
        return emptyToNull(getProperty(propertyName)) != null;
    }

    //TODO ADD GroupsAuthentication
    Authentication findAuthentication(String userID, OAuthServer server) {
        return users.stream().filter(a -> a.getUserID().equals(userID) && a
                .getServer().equals(server)).findFirst().orElse(null);
    }

    Authentication findAuthentication(BearerToken token) {
        if (securityDisabled) {
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

    //Compatibility things
    private void initServers() {
        if (securityDisabled) {
            return;
        }
        String[] serversTokens = getProperty(SECURITY_SERVERS).split(";");
        LinkedList<OAuthServer> temp = new LinkedList<>();

        for (String server : serversTokens) {
            String[] serverTokens = server.trim().split(",");
            temp.add(OAuthServer.builder().name(serverTokens[0]).attributeIDName(
                    serverTokens[1]).authURI(URI.create(serverTokens[2])).userInfoURI(URI
                    .create(serverTokens[3])).tokenEndpointURI(URI.create(
                    serverTokens[4])).clientID(serverTokens[5]).build());

        }


        servers = temp.stream().collect(Collectors.toMap(OAuthServer::getName,
                s -> s));
        System.out.println("inited servers"+servers.size());
    }
    private void initUsers() {
        if (securityDisabled) {
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

}
