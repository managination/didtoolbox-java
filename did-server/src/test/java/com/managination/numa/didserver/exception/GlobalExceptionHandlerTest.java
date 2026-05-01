package com.managination.numa.didserver.exception;

import com.managination.numa.didserver.dto.ErrorResponse;
import com.managination.numa.didserver.service.DidService;
import com.managination.numa.didserver.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void testDidNotFound() {
        DidService.DidNotFoundException ex = new DidService.DidNotFoundException("DID not found: did:webvh:test");

        ResponseEntity<ErrorResponse> response = handler.handleDidNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("not_found", response.getBody().error());
    }

    @Test
    void testSessionNotFound() {
        SessionService.SessionNotFoundException ex = new SessionService.SessionNotFoundException("Session not found: test-id");

        ResponseEntity<ErrorResponse> response = handler.handleSessionNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("not_found", response.getBody().error());
    }

    @Test
    void testIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("invalid_request", response.getBody().error());
    }
}
