package cz.it4i.fiji.datastore.security;

import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
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
    OAuthGroupRepository repository;

    @Inject
    OAuthUserNewRepository userNewRepository;

    public void createOAuthGroup(OAuthGroupDTO groupDTO) {
        OAuthGroup group = new OAuthGroup();
        group.setOwner((OAuthUserNew) OAuthUserNew.find(groupDTO.getOwnerId()));
        group.setName(groupDTO.getName());
        repository.persist(group);
    }

    public OAuthGroup getOAuthGroupById(Long id) {
        return repository.findById(id);
    }

    public List<OAuthGroup> getAllOAuthGroups() {
        return repository.listAll();
    }

    public void updateOAuthGroup(OAuthGroup group) {
        repository.update(String.valueOf(group));
    }

    public void deleteOAuthGroup(OAuthGroup group) {
        repository.delete(group);
    }

    public void addUserToGroup(long groupId, Long userId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        OAuthUserNew user = userNewRepository.findById(userId);
        group.addUser(user);
        repository.update(String.valueOf(group));
    }

    public boolean removeUserFromGroup(int groupId, Long userId) {
        OAuthGroup group = getOAuthGroupById((long) groupId);
        OAuthUserNew user = userNewRepository.findById(userId);
        boolean ok=group.removeUser(user);
        if(ok) {
            repository.update(String.valueOf(group));
            return true;
        }
        else{
            return false;
        }
    }

    public void addDatasetToGroup(long groupId, String datasetId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        DatasetRepository datasetRepository=new DatasetRepository();
        Dataset dataset = datasetRepository.findByUUID(datasetId);
        group.addDataset(dataset);
        repository.update(String.valueOf(group));
    }

    public void removeDatasetFromGroup(long groupId,String datasetId) {
        OAuthGroup group = getOAuthGroupById(groupId);
        DatasetRepository datasetRepository=new DatasetRepository();
        Dataset dataset = datasetRepository.findByUUID(datasetId);
        group.removeDataset(dataset);
        repository.update(String.valueOf(group));
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
                    PermissionType pt = PermissionType.fromString(ptChar+"");
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

interface OAuthGroupRepository extends PanacheRepository<OAuthGroup> {
}

interface OAuthUserNewRepository extends PanacheRepository<OAuthUserNew> {
}
