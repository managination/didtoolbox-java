package com.managination.numa.didserver.service;

import com.managination.numa.didserver.dto.CreateSessionRequest;
import com.managination.numa.didserver.dto.CreateSessionResponse;
import com.managination.numa.didserver.dto.SessionStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService();
    }

    @Test
    void testCreateSession() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        assertNotNull(response);
        assertNotNull(response.sessionId());
        assertNotNull(response.wsUrl());
        assertNotNull(response.qrCodeData());
        assertNotNull(response.expiresAt());
        assertTrue(response.wsUrl().contains(response.sessionId()));
    }

    @Test
    void testGetSessionStatus() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        SessionStatusResponse status = sessionService.getSessionStatus(response.sessionId());

        assertNotNull(status);
        assertEquals(response.sessionId(), status.sessionId());
        assertEquals("pending", status.status());
    }

    @Test
    void testGetSessionStatusNotFound() {
        assertThrows(SessionService.SessionNotFoundException.class, () -> {
            sessionService.getSessionStatus("non-existent-id");
        });
    }

    @Test
    void testCancelSession() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        sessionService.cancelSession(response.sessionId());

        SessionStatusResponse status = sessionService.getSessionStatus(response.sessionId());
        assertEquals("cancelled", status.status());
    }

    @Test
    void testCancelSessionNotFound() {
        assertThrows(SessionService.SessionNotFoundException.class, () -> {
            sessionService.cancelSession("non-existent-id");
        });
    }

    @Test
    void testSessionExists() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        assertTrue(sessionService.sessionExists(response.sessionId()));
        assertFalse(sessionService.sessionExists("non-existent-id"));
    }

    @Test
    void testExpireSession() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        sessionService.expireSession(response.sessionId());

        SessionStatusResponse status = sessionService.getSessionStatus(response.sessionId());
        assertEquals("expired", status.status());
    }

    @Test
    void testSetHolderDid() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        sessionService.setHolderDid(response.sessionId(), "did:webvh:test:holder");

        SessionStatusResponse status = sessionService.getSessionStatus(response.sessionId());
        assertEquals("did:webvh:test:holder", status.holderDid());
    }

    @Test
    void testActivateSession() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 300);
        CreateSessionResponse response = sessionService.createSession(request);

        sessionService.activateSession(response.sessionId());

        SessionStatusResponse status = sessionService.getSessionStatus(response.sessionId());
        assertEquals("holder_connected", status.status());
    }

    @Test
    void testGetExpiredSessionIds() {
        CreateSessionRequest request = new CreateSessionRequest("websocket", "jwt_vc_json", 0);
        CreateSessionResponse response = sessionService.createSession(request);

        Instant future = Instant.now().plusSeconds(1);
        List<String> expired = sessionService.getExpiredSessionIds(future);

        assertTrue(expired.contains(response.sessionId()));
    }
}
