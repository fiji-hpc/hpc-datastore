package cz.it4i.fiji.datastore.OAuthTests;

import cz.it4i.fiji.datastore.security.OAuthServer;
import cz.it4i.fiji.datastore.security.OAuthServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthServerServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OAuthServerService oAuthServerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void getOAuthServerById_ExistingId_ReturnsServer() {
        // Prepare
        Long id = 1L;
        OAuthServer expectedServer = new OAuthServer();
        when(entityManager.find(OAuthServer.class, id)).thenReturn(expectedServer);

        // Execute
        Optional<OAuthServer> actualServer = oAuthServerService.getOAuthServerById(id);

        // Verify
        assertEquals(Optional.of(expectedServer), actualServer);
        verify(entityManager).find(OAuthServer.class, id);
    }

    @Test
    void getOAuthServerById_NonexistentId_ReturnsEmptyOptional() {
        // Prepare
        Long id = 1L;
        when(entityManager.find(OAuthServer.class, id)).thenReturn(null);

        // Execute
        Optional<OAuthServer> actualServer = oAuthServerService.getOAuthServerById(id);

        // Verify
        assertEquals(Optional.empty(), actualServer);
        verify(entityManager).find(OAuthServer.class, id);
    }

    @Test
    void createOAuthServer_ShouldPersistServer() {
        OAuthServer server = new OAuthServer();
        boolean result = oAuthServerService.createOAuthServer(server);
        assertTrue(result);
    }

    @Test
    void updateOAuthServer_ExistingId_ShouldUpdateServer() {
        // Prepare
        Long id = 1L;
        OAuthServer existingServer = new OAuthServer();
        existingServer.setId(id);
        existingServer.setName("Old Name");

        OAuthServer updatedServer = new OAuthServer();
        updatedServer.setId(id);
        updatedServer.setName("New Name");

        when(entityManager.find(OAuthServer.class, id)).thenReturn(existingServer);

        // Execute
        boolean result = oAuthServerService.updateOAuthServer(id, updatedServer);

        // Verify
        assertTrue(result);
        assertEquals("New Name", existingServer.getName());
        verify(entityManager).merge(existingServer);
    }

    @Test
    void updateOAuthServer_NonexistentId_ShouldNotUpdateServer() {
        // Prepare
        Long id = 1L;
        OAuthServer updatedServer = new OAuthServer();
        updatedServer.setId(id);
        updatedServer.setName("New Name");

        when(entityManager.find(OAuthServer.class, id)).thenReturn(null);

        // Execute
        boolean result = oAuthServerService.updateOAuthServer(id, updatedServer);

        // Verify
        assertFalse(result);
        verify(entityManager, never()).merge(any(OAuthServer.class));
    }

    @Test
    void deleteOAuthServerById_ExistingId_ShouldRemoveServer() {
        // Prepare
        Long id = 1L;
        OAuthServer existingServer = new OAuthServer();
        when(entityManager.find(OAuthServer.class, id)).thenReturn(existingServer);

        // Execute
        boolean result = oAuthServerService.deleteOAuthServerById(id);

        // Verify
        assertTrue(result);
        verify(entityManager).remove(existingServer);
    }

    @Test
    void deleteOAuthServerById_NonexistentId_ShouldNotRemoveServer() {
        // Prepare
        Long id = 1L;
        when(entityManager.find(OAuthServer.class, id)).thenReturn(null);

        // Execute
        boolean result = oAuthServerService.deleteOAuthServerById(id);

        // Verify
        assertFalse(result);
        verify(entityManager, never()).remove(any(OAuthServer.class));
    }
}

