package cz.it4i.fiji.datastore.OAuthTests;
import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import cz.it4i.fiji.datastore.security.OAuthGroup;
import cz.it4i.fiji.datastore.security.OAuthGroupDTO;
import cz.it4i.fiji.datastore.security.OAuthGroupService;
import cz.it4i.fiji.datastore.security.PermissionType;
import cz.it4i.fiji.datastore.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthGroupServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OAuthGroupService oAuthGroupService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOAuthGroup_ShouldPersistGroup() {
        // Prepare
        OAuthGroupDTO groupDTO = new OAuthGroupDTO();
        groupDTO.setName("Test Group");

        User owner = new User();
        owner.setId(1L);

        when(entityManager.find(User.class, groupDTO.getOwnerId())).thenReturn(owner);

        // Execute
        oAuthGroupService.createOAuthGroup(groupDTO);

        // Verify
        verify(entityManager).persist(any(OAuthGroup.class));
    }

    @Test
    void getOAuthGroupById_ExistingId_ReturnsGroup() {
        // Prepare
        Long id = 1L;
        OAuthGroup expectedGroup = new OAuthGroup();
        when(entityManager.find(OAuthGroup.class, id)).thenReturn(expectedGroup);

        // Execute
        OAuthGroup actualGroup = oAuthGroupService.getOAuthGroupById(id);

        // Verify
        assertEquals(expectedGroup, actualGroup);
        verify(entityManager).find(OAuthGroup.class, id);
    }


    @Test
    void updateOAuthGroup_ShouldMergeGroup() {
        // Prepare
        OAuthGroup group = new OAuthGroup();

        // Execute
        oAuthGroupService.updateOAuthGroup(group);

        // Verify
        verify(entityManager).merge(group);
    }

    @Test
    void deleteOAuthGroup_ShouldRemoveGroup() {
        // Prepare
        OAuthGroup group = new OAuthGroup();

        // Execute
        oAuthGroupService.deleteOAuthGroup(group);

        // Verify
        verify(entityManager).remove(group);
    }

    @Test
    void addUserToGroup_ShouldAddUserToGroup() {
        // Prepare
        long groupId = 1L;
        Long userId = 2L;
        OAuthGroup group = new OAuthGroup();
        User user = new User();
        when(entityManager.find(OAuthGroup.class, groupId)).thenReturn(group);
        when(entityManager.find(User.class, userId)).thenReturn(user);

        // Execute
        oAuthGroupService.addUserToGroup(groupId, userId);

        // Verify
        assertTrue(group.getUsers().contains(user));
        verify(entityManager).merge(group);
    }

    @Test
    void removeUserFromGroup_ExistingUser_ShouldRemoveUserFromGroup() {
        // Prepare
        int groupId = 1;
        Long userId = 2L;
        OAuthGroup group = new OAuthGroup();
        User user = new User();
        group.addUser(user);
        when(entityManager.find(OAuthGroup.class, (long) groupId)).thenReturn(group);
        when(entityManager.find(User.class, userId)).thenReturn(user);

        // Execute
        boolean result = oAuthGroupService.removeUserFromGroup(groupId, userId);

        // Verify
        assertTrue(result);
        assertFalse(group.getUsers().contains(user));
        verify(entityManager).merge(group);
    }

    @Test
    void removeUserFromGroup_NonexistentUser_ShouldNotRemoveUserFromGroup() {
        // Prepare
        int groupId = 1;
        Long userId = 2L;
        OAuthGroup group = new OAuthGroup();
        when(entityManager.find(OAuthGroup.class, (long) groupId)).thenReturn(group);
        when(entityManager.find(User.class, userId)).thenReturn(null);

        // Execute
        boolean result = oAuthGroupService.removeUserFromGroup(groupId, userId);

        // Verify
        assertFalse(result);
        verify(entityManager, never()).merge(any(OAuthGroup.class));
    }

    @Test
    void changeGroupPermission_InvalidPermissionType_ReturnsFalse() {
        long groupId = 1L;
        String permissionType = "ABC";

        OAuthGroup group = mock(OAuthGroup.class);
        when(entityManager.find(OAuthGroup.class, groupId)).thenReturn(group);
        assertThrows(IllegalArgumentException.class, () -> {
            oAuthGroupService.changeGroupPermission(groupId, permissionType);
        });

        verify(entityManager, never()).merge(any(OAuthGroup.class));
    }



    @Test
    void changeGroupPermission_ValidPermissionType_ShouldChangePermission() {
        // Prepare
        long groupId = 1L;
        String permissionType = "R";
        OAuthGroup group = new OAuthGroup();
        when(entityManager.find(OAuthGroup.class, groupId)).thenReturn(group);

        // Execute
        boolean result = oAuthGroupService.changeGroupPermission(groupId, permissionType);

        // Verify
        assertTrue(result);
        assertEquals(EnumSet.of(PermissionType.R), group.getPermissionType());
        verify(entityManager).merge(group);
    }
}
