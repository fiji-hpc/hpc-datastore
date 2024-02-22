package cz.it4i.fiji.datastore.security;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Default
@Slf4j
public class OAuthUserService {

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<User> getAllOAuthUsers() {

        TypedQuery<User> query = entityManager.createQuery("SELECT s FROM User s", User.class);
        return query.getResultList();
    }

    public Optional<User> getOAuthUserById(Long id) {
        User oAuthServer = entityManager.find(User.class, id);
        return Optional.ofNullable(oAuthServer);
    }

    @Transactional
    public void createOAuthUser(User oAuthServer) {
        entityManager.persist(oAuthServer);
    }

    @Transactional
    public void updateOAuthServer(Long id,User oAuthServer) {
        entityManager.merge(oAuthServer);
    }

    @Transactional
    public void deleteOAuthUserById(Long id) {
        Optional<User> oAuthServer = getOAuthUserById(id);
        oAuthServer.ifPresent(entityManager::remove);
    }
}