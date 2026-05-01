package com.managination.numa.didserver.service;

import com.managination.numa.didserver.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private record SessionInfo(
        String sessionId,
        String transportType,
        String credentialType,
        Instant createdAt,
        Instant expiresAt,
        SessionStatus status,
        String holderDid
    ) {
        SessionInfo withHolderDid(String holderDid) {
            return new SessionInfo(sessionId, transportType, credentialType, createdAt, expiresAt, status, holderDid);
        }

        SessionInfo withStatus(SessionStatus status) {
            return new SessionInfo(sessionId, transportType, credentialType, createdAt, expiresAt, status, holderDid);
        }
    }

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public CreateSessionResponse createSession(CreateSessionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(request.getTimeoutOrDefault());

        SessionInfo session = new SessionInfo(
            sessionId,
            request.transportType(),
            request.credentialType(),
            now,
            expiresAt,
            SessionStatus.PENDING,
            null
        );

        sessions.put(sessionId, session);

        String wsUrl = "ws://localhost:8080/ws/session/" + sessionId;
        String qrCodeData = wsUrl;

        return new CreateSessionResponse(sessionId, wsUrl, qrCodeData, expiresAt);
    }

    public SessionStatusResponse getSessionStatus(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionId);
        }

        if (Instant.now().isAfter(session.expiresAt()) && SessionStatus.PENDING.equals(session.status())) {
            sessions.computeIfPresent(sessionId, (k, v) ->
                new SessionInfo(v.sessionId(), v.transportType(), v.credentialType(),
                    v.createdAt(), v.expiresAt(), SessionStatus.EXPIRED, v.holderDid()));
            return new SessionStatusResponse(
                sessionId, SessionStatus.EXPIRED.getValue(), session.transportType(), session.holderDid(),
                session.createdAt(), session.expiresAt()
            );
        }

        return new SessionStatusResponse(
            session.sessionId(),
            session.status().getValue(),
            session.transportType(),
            session.holderDid(),
            session.createdAt(),
            session.expiresAt()
        );
    }

    public void cancelSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionId);
        }

        sessions.computeIfPresent(sessionId, (k, v) -> v.withStatus(SessionStatus.CANCELLED));
        log.info("Session cancelled: {}", sessionId);
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void expireSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            return;
        }

        sessions.computeIfPresent(sessionId, (k, v) -> v.withStatus(SessionStatus.EXPIRED));
        log.info("Session expired: {}", sessionId);
    }

    public List<String> getExpiredSessionIds(Instant now) {
        return sessions.values().stream()
            .filter(s -> SessionStatus.PENDING.equals(s.status()) || SessionStatus.HOLDER_CONNECTED.equals(s.status()) || SessionStatus.CREDENTIAL_ISSUED.equals(s.status()))
            .filter(s -> now.isAfter(s.expiresAt()))
            .map(SessionInfo::sessionId)
            .collect(Collectors.toList());
    }

    public void setHolderDid(String sessionId, String holderDid) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionId);
        }

        sessions.computeIfPresent(sessionId, (k, v) -> v.withHolderDid(holderDid));
        log.info("Holder DID set for session {}: {}", sessionId, holderDid);
    }

    public void activateSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionId);
        }

        if (SessionStatus.PENDING.equals(session.status())) {
            sessions.computeIfPresent(sessionId, (k, v) -> v.withStatus(SessionStatus.HOLDER_CONNECTED));
            log.info("Session activated: {}", sessionId);
        }
    }

    public void setCredentialIssued(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionId);
        }

        sessions.computeIfPresent(sessionId, (k, v) -> v.withStatus(SessionStatus.CREDENTIAL_ISSUED));
        log.info("Credential issued for session: {}", sessionId);
    }

    public void setCompleted(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException("Session not found: " + sessionId);
        }

        sessions.computeIfPresent(sessionId, (k, v) -> v.withStatus(SessionStatus.COMPLETED));
        log.info("Session completed: {}", sessionId);
    }

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String message) {
            super(message);
        }
    }
}
