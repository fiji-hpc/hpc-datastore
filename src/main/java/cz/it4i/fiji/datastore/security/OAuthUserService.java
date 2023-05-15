package cz.it4i.fiji.datastore.security;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Default
@Slf4j
public class OAuthUserService {

    @Inject
    EntityManager entityManager;

    public List<OAuthUserNew> getAllOAuthUsers() {

         return OAuthUserNew.findAll().list();
    }

    public Optional<OAuthUserNew> getOAuthUserById(Long id) {
        OAuthUserNew oAuthServer = entityManager.find(OAuthUserNew.class, id);
        return Optional.ofNullable(oAuthServer);
    }

    @Transactional
    public void createOAuthUser(OAuthUserNew oAuthServer) {
        entityManager.persist(oAuthServer);
    }

    @Transactional
    public void updateOAuthServer(Long id,OAuthUserNew oAuthServer) {
        entityManager.merge(oAuthServer);
    }

    @Transactional
    public void deleteOAuthUserById(Long id) {
        Optional<OAuthUserNew> oAuthServer = getOAuthUserById(id);
        oAuthServer.ifPresent(entityManager::remove);
    }
}