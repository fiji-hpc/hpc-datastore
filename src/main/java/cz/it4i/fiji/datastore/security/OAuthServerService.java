package cz.it4i.fiji.datastore.security;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
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
        TypedQuery<OAuthServer> query = entityManager.createQuery("SELECT s FROM OAuthServer s", OAuthServer.class);
        return query.getResultList();
    }

    public Optional<OAuthServer> getOAuthServerById(Long id) {
        return Optional.ofNullable(entityManager.find(OAuthServer.class, id));
    }
    @Transactional
    public boolean createOAuthServer(OAuthServer oAuthServer) {
        OAuthServer newOAuthServer = new OAuthServer();
        newOAuthServer.setAuthURI(oAuthServer.getAuthURI());
        newOAuthServer.setRedirectURI(oAuthServer.getRedirectURI());
        newOAuthServer.setUserInfoURI(oAuthServer.getUserInfoURI());
        newOAuthServer.setTokenEndpointURI(oAuthServer.getTokenEndpointURI());
        newOAuthServer.setClientID(oAuthServer.getClientID());
        newOAuthServer.setClientSecret(oAuthServer.getClientSecret());
        newOAuthServer.setName(oAuthServer.getName());
        newOAuthServer.setAttributeIDName(oAuthServer.getAttributeIDName());

        entityManager.persist(newOAuthServer);
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
