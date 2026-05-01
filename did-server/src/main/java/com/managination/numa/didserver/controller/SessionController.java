package com.managination.numa.didserver.controller;

import com.managination.numa.didserver.dto.*;
import com.managination.numa.didserver.service.SessionService;
import com.managination.numa.didserver.websocket.SessionWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@Tag(name = "Sessions", description = "WebSocket session management for peer-to-peer credential issuance")
public class SessionController {

    private final SessionService sessionService;
    private final SessionWebSocketHandler webSocketHandler;

    public SessionController(SessionService sessionService, SessionWebSocketHandler webSocketHandler) {
        this.sessionService = sessionService;
        this.webSocketHandler = webSocketHandler;
    }

    @PostMapping
    @Operation(summary = "Create a new credential issuance session", operationId = "createSession")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Session created successfully",
            content = @Content(schema = @Schema(implementation = CreateSessionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid session request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request) {
        if (request.transportType() == null || request.transportType().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", "transportType is required"));
        }

        CreateSessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session status", operationId = "getSessionStatus")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session status retrieved",
            content = @Content(schema = @Schema(implementation = SessionStatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Session not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> getSessionStatus(
            @Parameter(description = "The session UUID", required = true)
            @PathVariable String sessionId) {
        try {
            SessionStatusResponse response = sessionService.getSessionStatus(sessionId);
            return ResponseEntity.ok(response);
        } catch (SessionService.SessionNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Cancel a session", operationId = "cancelSession")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Session cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> cancelSession(
            @Parameter(description = "The session UUID", required = true)
            @PathVariable String sessionId) {
        try {
            sessionService.cancelSession(sessionId);
            webSocketHandler.notifySessionCancelled(sessionId);
            return ResponseEntity.noContent().build();
        } catch (SessionService.SessionNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", e.getMessage()));
        }
    }
}
