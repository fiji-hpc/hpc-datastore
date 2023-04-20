package cz.it4i.fiji.datastore.security;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Default
@Slf4j

public class OAuthServerService {

    @Inject
    EntityManager entityManager;

    public List<OAuthServerNew> getAllOAuthServers() {
        if(entityManager==null)
        {
            return new ArrayList<OAuthServerNew>();
        }
        return entityManager.createQuery("SELECT os FROM OAuthServerNew os", OAuthServerNew.class)
                .getResultList();
       // return  new DatabaseHandler().findServers();
    }

    public Optional<OAuthServerNew> getOAuthServerById(Long id) {
        OAuthServerNew oAuthServer = entityManager.find(OAuthServerNew.class, id);
        return Optional.ofNullable(oAuthServer);
    }

    @Transactional
    public void createOAuthServer(OAuthServerNew oAuthServer) {
        entityManager.persist(oAuthServer);
    }

    @Transactional
    public void updateOAuthServer(Long id,OAuthServerNew oAuthServer) {
        entityManager.merge(oAuthServer);
    }

    @Transactional
    public void deleteOAuthServerById(Long id) {
        Optional<OAuthServerNew> oAuthServer = getOAuthServerById(id);
        oAuthServer.ifPresent(entityManager::remove);
    }


}