package com.managination.numa.didserver.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managination.numa.didserver.dto.websocket.*;
import com.managination.numa.didserver.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SessionWebSocketHandler.class);
    private static final Set<String> VALID_CREDENTIAL_FORMATS = Set.of(
        "jwt_vc_json",
        "jwt_vc_json-ld",
        "ldp_vc"
    );
    private static final UriTemplate SESSION_URI_TEMPLATE = new UriTemplate("/ws/session/{sessionId}");
    private static final int MAX_MESSAGE_SIZE = 64 * 1024;

    private final ObjectMapper objectMapper;
    private final SessionService sessionService;

    private final Map<String, WebSocketSession> issuerSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> holderSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRoles = new ConcurrentHashMap<>();

    public SessionWebSocketHandler(ObjectMapper objectMapper, SessionService sessionService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        if (sessionId == null) {
            sendErrorAndClose(session, "unknown", "SESSION_NOT_FOUND", "Invalid session URL");
            return;
        }

        if (!sessionService.sessionExists(sessionId)) {
            sendErrorAndClose(session, sessionId, "SESSION_NOT_FOUND", "Session does not exist");
            return;
        }

        log.info("WebSocket connection established for session: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        if (sessionId == null) {
            sendErrorAndClose(session, "unknown", "SESSION_NOT_FOUND", "Invalid session URL");
            return;
        }

        if (message.getPayloadLength() > MAX_MESSAGE_SIZE) {
            sendErrorAndClose(session, sessionId, "MESSAGE_TOO_LARGE", "Message exceeds maximum allowed size");
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : null;

            if (type == null) {
                sendError(session, sessionId, "INVALID_MESSAGE", "Message type is required");
                return;
            }

            String payloadSessionId = jsonNode.has("sessionId") ? jsonNode.get("sessionId").asText() : null;
            if (payloadSessionId != null && !payloadSessionId.equals(sessionId)) {
                sendError(session, sessionId, "INVALID_MESSAGE", "sessionId in message does not match URI");
                return;
            }

            switch (type) {
                case "join" -> handleJoin(session, sessionId, jsonNode);
                case "holder_did" -> handleHolderDid(session, sessionId, jsonNode);
                case "credential" -> handleCredential(session, sessionId, jsonNode);
                default -> sendError(session, sessionId, "INVALID_MESSAGE", "Unknown message type: " + type);
            }
        } catch (SessionService.SessionNotFoundException e) {
            log.error("Session not found: {}", sessionId);
            sendError(session, sessionId, "SESSION_NOT_FOUND", "Session does not exist");
        } catch (Exception e) {
            log.error("Error processing message for session {}: {}", sessionId, e.getMessage());
            sendError(session, sessionId, "INVALID_MESSAGE", "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        if (sessionId != null) {
            String role = sessionRoles.remove(session.getId());
            if ("issuer".equals(role)) {
                issuerSessions.remove(sessionId);
                WebSocketSession holderSession = holderSessions.get(sessionId);
                if (holderSession != null && holderSession.isOpen()) {
                    sendMessage(holderSession, new SessionCancelledMessage(sessionId));
                    holderSession.close(CloseStatus.NORMAL);
                }
                sessionService.cancelSession(sessionId);
            } else if ("holder".equals(role)) {
                holderSessions.remove(sessionId);
                WebSocketSession issuerSession = issuerSessions.get(sessionId);
                if (issuerSession != null && issuerSession.isOpen()) {
                    sendMessage(issuerSession, new ErrorMessage(sessionId, "INTERNAL_ERROR", "Holder disconnected"));
                }
            }
            log.info("WebSocket connection closed for session {} (role: {})", sessionId, role);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        log.error("Transport error for session {}: {}", sessionId, exception.getMessage());
        if (sessionId != null) {
            sendError(session, sessionId, "INTERNAL_ERROR", "Transport error occurred");
        }
    }

    private void handleJoin(WebSocketSession session, String sessionId, JsonNode jsonNode) throws IOException {
        String role = jsonNode.has("role") ? jsonNode.get("role").asText() : null;
        if (role == null || (!role.equals("issuer") && !role.equals("holder"))) {
            sendError(session, sessionId, "ROLE_MISMATCH", "Role must be 'issuer' or 'holder'");
            return;
        }

        String existingRole = sessionRoles.get(session.getId());
        if (existingRole != null) {
            sendError(session, sessionId, "DUPLICATE_JOIN", "Session already joined as " + existingRole);
            return;
        }

        if (role.equals("issuer")) {
            if (issuerSessions.containsKey(sessionId)) {
                sendError(session, sessionId, "DUPLICATE_JOIN", "Issuer already connected to this session");
                return;
            }
            issuerSessions.put(sessionId, session);
            sessionRoles.put(session.getId(), "issuer");
            log.info("Issuer joined session: {}", sessionId);

            if (holderSessions.containsKey(sessionId)) {
                sessionService.activateSession(sessionId);
                sendToIssuer(sessionId, new SessionReadyMessage(sessionId));
            }
        } else {
            if (holderSessions.containsKey(sessionId)) {
                sendError(session, sessionId, "DUPLICATE_JOIN", "Holder already connected to this session");
                return;
            }
            holderSessions.put(sessionId, session);
            sessionRoles.put(session.getId(), "holder");
            log.info("Holder joined session: {}", sessionId);

            if (issuerSessions.containsKey(sessionId)) {
                sessionService.activateSession(sessionId);
                sendToIssuer(sessionId, new SessionReadyMessage(sessionId));
            }
        }
    }

    private void handleHolderDid(WebSocketSession session, String sessionId, JsonNode jsonNode) throws IOException {
        String role = sessionRoles.get(session.getId());
        if (!"holder".equals(role)) {
            sendError(session, sessionId, "UNAUTHORIZED", "Only holder can send holder_did message");
            return;
        }

        String did = jsonNode.has("did") ? jsonNode.get("did").asText() : null;
        if (did == null || !did.startsWith("did:webvh:")) {
            sendError(session, sessionId, "INVALID_DID", "Valid DID starting with did:webvh: is required");
            return;
        }

        sessionService.setHolderDid(sessionId, did);
        sendToIssuer(sessionId, new HolderDidMessage(sessionId, did));
        log.info("Holder DID relayed to issuer for session {}: {}", sessionId, did);
    }

    private void handleCredential(WebSocketSession session, String sessionId, JsonNode jsonNode) throws IOException {
        String role = sessionRoles.get(session.getId());
        if (!"issuer".equals(role)) {
            sendError(session, sessionId, "UNAUTHORIZED", "Only issuer can send credential message");
            return;
        }

        String credential = jsonNode.has("credential") ? jsonNode.get("credential").asText() : null;
        if (credential == null || credential.isBlank()) {
            sendError(session, sessionId, "INVALID_CREDENTIAL", "Credential is required");
            return;
        }

        String format = jsonNode.has("format") ? jsonNode.get("format").asText() : "jwt_vc_json";
        if (!VALID_CREDENTIAL_FORMATS.contains(format)) {
            sendError(session, sessionId, "INVALID_FORMAT", "Unsupported credential format: " + format + ". Must be one of: " + String.join(", ", VALID_CREDENTIAL_FORMATS));
            return;
        }
        sendToHolder(sessionId, new CredentialMessage(sessionId, credential, format));
        sessionService.setCredentialIssued(sessionId);
        log.info("Credential relayed to holder for session: {}", sessionId);
    }

    private void sendToIssuer(String sessionId, Object message) throws IOException {
        WebSocketSession issuerSession = issuerSessions.get(sessionId);
        if (issuerSession != null && issuerSession.isOpen()) {
            sendMessage(issuerSession, message);
        } else {
            log.warn("No open issuer session for session: {}", sessionId);
        }
    }

    private void sendToHolder(String sessionId, Object message) throws IOException {
        WebSocketSession holderSession = holderSessions.get(sessionId);
        if (holderSession != null && holderSession.isOpen()) {
            sendMessage(holderSession, message);
            sessionService.setCompleted(sessionId);
            sendToIssuer(sessionId, new CredentialReceivedMessage(sessionId));
        } else {
            log.warn("No open holder session for session: {}", sessionId);
        }
    }

    private void sendMessage(WebSocketSession session, Object message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    private void sendError(WebSocketSession session, String sessionId, String code, String message) throws IOException {
        ErrorMessage errorMessage = new ErrorMessage(sessionId, code, message);
        String json = objectMapper.writeValueAsString(errorMessage);
        session.sendMessage(new TextMessage(json));
    }

    private void sendErrorAndClose(WebSocketSession session, String sessionId, String code, String message) throws IOException {
        sendError(session, sessionId, code, message);
        session.close(CloseStatus.POLICY_VIOLATION);
    }

    private String extractSessionId(URI uri) {
        if (uri == null) {
            return null;
        }
        Map<String, String> variables = SESSION_URI_TEMPLATE.match(uri.getPath());
        return variables.get("sessionId");
    }

    public void notifySessionCancelled(String sessionId) {
        WebSocketSession holderSession = holderSessions.get(sessionId);
        if (holderSession != null && holderSession.isOpen()) {
            try {
                sendMessage(holderSession, new SessionCancelledMessage(sessionId));
                holderSession.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.error("Failed to send session cancelled notification for session: {}", sessionId, e);
            }
        }
        holderSessions.remove(sessionId);
        issuerSessions.remove(sessionId);
    }

    public void notifySessionExpired(String sessionId) {
        sessionService.expireSession(sessionId);
        SessionExpiredMessage message = new SessionExpiredMessage(sessionId);
        WebSocketSession issuerSession = issuerSessions.get(sessionId);
        WebSocketSession holderSession = holderSessions.get(sessionId);

        try {
            if (issuerSession != null && issuerSession.isOpen()) {
                sendMessage(issuerSession, message);
                issuerSession.close(CloseStatus.NORMAL);
            }
        } catch (IOException e) {
            log.error("Failed to send session expired notification to issuer for session: {}", sessionId, e);
        }

        try {
            if (holderSession != null && holderSession.isOpen()) {
                sendMessage(holderSession, message);
                holderSession.close(CloseStatus.NORMAL);
            }
        } catch (IOException e) {
            log.error("Failed to send session expired notification to holder for session: {}", sessionId, e);
        }

        issuerSessions.remove(sessionId);
        holderSessions.remove(sessionId);
    }
}
