package cz.it4i.fiji.datastore.security;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Transactional
@ApplicationScoped
@Default
@Slf4j
public class OAuthGroupService {

    @PersistenceContext
    EntityManager entityManager;

    public void createOAuthGroup(OAuthGroup group) {
        entityManager.persist(group);
    }

    public OAuthGroup getOAuthGroupById(int id) {
        return entityManager.find(OAuthGroup.class, id);
    }

    public List<OAuthGroup> getAllOAuthGroups() {
        TypedQuery<OAuthGroup> query = entityManager.createQuery("SELECT g FROM OAuthGroup g", OAuthGroup.class);
        return query.getResultList();
    }

    public void updateOAuthGroup(OAuthGroup group) {
        entityManager.merge(group);
    }

    public void deleteOAuthGroup(OAuthGroup group) {
        entityManager.remove(group);
    }

    public void addUserToGroup(int groupId, int userId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        group.addUser(userId);
        entityManager.merge(group);
    }

    public void removeUserFromGroup(int groupId, int userId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        group.deleteUsers(userId);
        entityManager.merge(group);
    }

    public void addDatasetToGroup(int groupId, UUID datasetId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        group.addDatasetbyUUID(datasetId);
        entityManager.merge(group);
    }

    public void removeDatasetFromGroup(int groupId, UUID datasetId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        group.deleteDatasetbyUUID(datasetId);
        entityManager.merge(group);
    }

    public void changeGroupPermission(int groupId, PermissionType permissionType) {
        OAuthGroup group = getOAuthGroupById(groupId);
        group.changePermission(permissionType);
        entityManager.merge(group);
    }
}
