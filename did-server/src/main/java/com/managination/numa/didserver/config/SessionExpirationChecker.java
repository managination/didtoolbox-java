package com.managination.numa.didserver.config;

import com.managination.numa.didserver.service.SessionService;
import com.managination.numa.didserver.websocket.SessionWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class SessionExpirationChecker {

    private static final Logger log = LoggerFactory.getLogger(SessionExpirationChecker.class);
    private static final long CHECK_INTERVAL_MS = 30_000;

    private final SessionService sessionService;
    private final SessionWebSocketHandler webSocketHandler;
    private final Map<String, Boolean> notifiedSessions = new ConcurrentHashMap<>();

    public SessionExpirationChecker(SessionService sessionService, SessionWebSocketHandler webSocketHandler) {
        this.sessionService = sessionService;
        this.webSocketHandler = webSocketHandler;
    }

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void checkExpiredSessions() {
        Instant now = Instant.now();
        sessionService.getExpiredSessionIds(now).forEach(sessionId -> {
            if (notifiedSessions.putIfAbsent(sessionId, Boolean.TRUE) == null) {
                log.info("Notifying clients of expired session: {}", sessionId);
                webSocketHandler.notifySessionExpired(sessionId);
            }
        });
    }
}
