package com.managination.numa.didserver.service;

import com.managination.numa.didserver.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DidServiceTest {

    private DidService didService;

    @BeforeEach
    void setUp() {
        didService = new DidService();
        didService.init();
    }

    @Test
    void testGetHealthStatus() {
        HealthStatus health = didService.getHealthStatus();

        assertNotNull(health);
        assertNotNull(health.status());
        assertNotNull(health.timestamp());
        assertNotNull(health.filesystem());
        assertNotNull(health.memory());
        assertNotNull(health.jvm());
        assertNotNull(health.environment());
    }

    @Test
    void testGetServerPublicKey() {
        ServerPublicKeyResponse response = didService.getServerPublicKey();

        assertNotNull(response);
        assertNotNull(response.publicKey());
        assertEquals("ES256", response.algorithm());
        assertNotNull(response.keyId());
        assertNotNull(response.rotatedAt());
    }

    @Test
    void testRegisterDid() {
        assertThrows(RuntimeException.class, () -> {
            DidRegistrationRequest request = new DidRegistrationRequest(
                "did:webvh:test:register.example.com",
                new com.managination.numa.didserver.model.DidDocument(
                    java.util.List.of("https://www.w3.org/ns/did/v1"),
                    "did:webvh:test:register.example.com",
                    null, null, null, null, null, null, null, null, null
                ),
                java.util.List.of(java.util.Map.of(
                    "versionId", "1-testhash",
                    "versionTime", "2024-01-01T00:00:00Z",
                    "state", java.util.Map.of("id", "did:webvh:test:register.example.com")
                ))
            );
            didService.registerDid(request);
        });
    }

    @Test
    void testResolveDid() {
        assertThrows(DidService.DidNotFoundException.class, () -> {
            didService.resolveDid("did:webvh:nonexistent:example.com");
        });
    }

    @Test
    void testUpdateDid() {
        assertThrows(DidService.DidNotFoundException.class, () -> {
            com.managination.numa.didserver.model.DidDocument doc = new com.managination.numa.didserver.model.DidDocument(
                java.util.List.of("https://www.w3.org/ns/did/v1"),
                "did:webvh:nonexistent:example.com",
                null, null, null, null, null, null, null, null, null
            );
            DidUpdateRequest request = new DidUpdateRequest(doc, null, "0");
            didService.updateDid("did:webvh:nonexistent:example.com", request);
        });
    }

    @Test
    void testVersionConflict() {
        assertThrows(DidService.DidNotFoundException.class, () -> {
            com.managination.numa.didserver.model.DidDocument doc = new com.managination.numa.didserver.model.DidDocument(
                java.util.List.of("https://www.w3.org/ns/did/v1"),
                "did:webvh:nonexistent:example.com",
                null, null, null, null, null, null, null, null, null
            );
            DidUpdateRequest request = new DidUpdateRequest(doc, null, "wrong-version");
            didService.updateDid("did:webvh:nonexistent:example.com", request);
        });
    }
}
