package cz.it4i.fiji.datastore.OAuthTests;

import cz.it4i.fiji.datastore.security.OAuthUserService;
import cz.it4i.fiji.datastore.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OAuthUserServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OAuthUserService oAuthUserService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void getOAuthUserById_ExistingId_ShouldReturnUser() {
        // Prepare
        Long id = 1L;
        User expectedUser = new User();
        when(entityManager.find(User.class, id)).thenReturn(expectedUser);

        // Execute
        Optional<User> actualUser = oAuthUserService.getOAuthUserById(id);

        // Verify
        assertEquals(Optional.of(expectedUser), actualUser);
        verify(entityManager).find(User.class, id);
    }

    @Test
    void getOAuthUserById_NonexistentId_ShouldReturnEmptyOptional() {
        // Prepare
        Long id = 1L;
        when(entityManager.find(User.class, id)).thenReturn(null);

        // Execute
        Optional<User> actualUser = oAuthUserService.getOAuthUserById(id);

        // Verify
        assertEquals(Optional.empty(), actualUser);
        verify(entityManager).find(User.class, id);
    }

    @Test
    void createOAuthUser_ShouldPersistUser() {
        // Prepare
        User user = new User();

        // Execute
        oAuthUserService.createOAuthUser(user);

        // Verify
        verify(entityManager).persist(user);
    }

    @Test
    void updateOAuthServer_ShouldMergeUser() {
        // Prepare
        Long id = 1L;
        User user = new User();

        // Execute
        oAuthUserService.updateOAuthServer(id, user);

        // Verify
        verify(entityManager).merge(user);
    }

    @Test
    void deleteOAuthUserById_ExistingId_ShouldRemoveUser() {
        // Prepare
        Long id = 1L;
        User user = new User();
        when(entityManager.find(User.class, id)).thenReturn(user);

        // Execute
        oAuthUserService.deleteOAuthUserById(id);

        // Verify
        verify(entityManager).remove(user);
    }

    @Test
    void deleteOAuthUserById_NonexistentId_ShouldDoNothing() {
        // Prepare
        Long id = 1L;
        when(entityManager.find(User.class, id)).thenReturn(null);

        // Execute
        oAuthUserService.deleteOAuthUserById(id);

        // Verify
        verify(entityManager, never()).remove(any(User.class));
    }
}

