package cz.it4i.fiji.datastore.security;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Default
@Slf4j
public class OAuthServerService {

    @Inject
    EntityManager entityManager;

    public List<OAuthServer> getAllOAuthServers() {
        if (entityManager == null) {
            return Collections.emptyList();
        }
        return OAuthServer.listAll();
    }

    public Optional<OAuthServer> getOAuthServerById(Long id) {
        return Optional.ofNullable(entityManager.find(OAuthServer.class, id));
    }

    @Transactional
    public boolean createOAuthServer(OAuthServer oAuthServer) {
        entityManager.persist(oAuthServer);
        return true;
    }

    @Transactional
    public boolean updateOAuthServer(Long id,OAuthServer oAuthServer) {
        OAuthServer existingServer = entityManager.find(OAuthServer.class, id);
        if (existingServer != null) {
            existingServer.setName(oAuthServer.getName());
            existingServer.setAuthURI(oAuthServer.getAuthURI());
            existingServer.setRedirectURI(oAuthServer.getRedirectURI());
            existingServer.setTokenEndpointURI(oAuthServer.getTokenEndpointURI());
            existingServer.setUserInfoURI(oAuthServer.getUserInfoURI());
            entityManager.merge(existingServer);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public boolean deleteOAuthServerById(Long id) {
        OAuthServer existingServer = entityManager.find(OAuthServer.class, id);
        if (existingServer != null) {
            entityManager.remove(existingServer);
            return true;
        } else {
            return false;
        }
    }
}
