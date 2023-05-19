package cz.it4i.fiji.datastore.security;

import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Transactional
@ApplicationScoped
@Default
@Slf4j
public class OAuthGroupService {
    @Inject
    EntityManager entityManager;

    public void createOAuthGroup(OAuthGroupDTO groupDTO) {
        OAuthGroup group = new OAuthGroup();
        group.setOwner((User) User.find(groupDTO.getOwnerId()));
        group.setName(groupDTO.getName());
        entityManager.persist(group);
    }

    public OAuthGroup getOAuthGroupById(Long id) {
        return entityManager.find(OAuthGroup.class, id);
    }

    public List<OAuthGroup> getAllOAuthGroups() {
        return entityManager.createQuery("SELECT g FROM OAuthGroup g", OAuthGroup.class)
                .getResultList();
    }

    public void updateOAuthGroup(OAuthGroup group) {
        entityManager.merge(group);
    }

    public void deleteOAuthGroup(OAuthGroup group) {
        entityManager.remove(group);
    }

    public void addUserToGroup(long groupId, Long userId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        User user = entityManager.find(User.class, userId);
        group.addUser(user);
        entityManager.merge(group);
    }

    public boolean removeUserFromGroup(int groupId, Long userId) {
        OAuthGroup group = getOAuthGroupById((long) groupId);
        User user = entityManager.find(User.class, userId);
        boolean ok = group.removeUser(user);
        if (ok) {
            entityManager.merge(group);
            return true;
        } else {
            return false;
        }
    }

    public void addDatasetToGroup(long groupId, String datasetId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        DatasetRepository datasetRepository = new DatasetRepository();
        Dataset dataset = datasetRepository.findByUUID(datasetId);
        group.addDataset(dataset);
        entityManager.merge(group);
    }

    public void removeDatasetFromGroup(long groupId, String datasetId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        DatasetRepository datasetRepository = new DatasetRepository();
        Dataset dataset = datasetRepository.findByUUID(datasetId);
        group.removeDataset(dataset);
        entityManager.merge(group);
    }

    public boolean changeGroupPermission(long groupId, String permissionType) {
        OAuthGroup group = getOAuthGroupById(groupId);
        EnumSet<PermissionType> ptSet = EnumSet.noneOf(PermissionType.class);
        if (group != null) {
            if (permissionType.length() > 3 || permissionType.length() < 1) {
                return false;
            } else {
                for (int i = 0; i < permissionType.length(); i++) {
                    char ptChar = permissionType.charAt(i);
                    PermissionType pt = PermissionType.fromString(ptChar + "");
                    if (pt != null) {
                        ptSet.add(pt);
                    } else {
                        return false;
                    }
                }
            }
            group.setPermissionType(ptSet);
            updateOAuthGroup(group);
            return true;
        } else {
            return false;
        }
    }
}
