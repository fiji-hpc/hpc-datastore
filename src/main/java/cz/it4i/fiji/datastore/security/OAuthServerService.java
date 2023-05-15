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

    public List<OAuthServerNew> getAllOAuthServers() {
        if (entityManager == null) {
            return Collections.emptyList();
        }
        return OAuthServerNew.listAll();
    }

    public Optional<OAuthServerNew> getOAuthServerById(Long id) {
        return Optional.ofNullable(entityManager.find(OAuthServerNew.class, id));
    }

    @Transactional
    public boolean createOAuthServer(OAuthServerNew oAuthServer) {
        entityManager.persist(oAuthServer);
        return true;
    }

    @Transactional
    public boolean updateOAuthServer(Long id, OAuthServerNew oAuthServer) {
        OAuthServerNew existingServer = entityManager.find(OAuthServerNew.class, id);
        if (existingServer != null) {
            existingServer.setAlias(oAuthServer.getAlias());
            existingServer.setSub(oAuthServer.getSub());
            existingServer.setLink_oauth(oAuthServer.getLink_oauth());
            existingServer.setLink_user_info(oAuthServer.getLink_user_info());
            existingServer.setLink_token(oAuthServer.getLink_token());
            entityManager.merge(existingServer);
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public boolean deleteOAuthServerById(Long id) {
        OAuthServerNew existingServer = entityManager.find(OAuthServerNew.class, id);
        if (existingServer != null) {
            entityManager.remove(existingServer);
            return true;
        } else {
            return false;
        }
    }
}
