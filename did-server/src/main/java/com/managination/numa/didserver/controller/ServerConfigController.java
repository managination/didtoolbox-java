package com.managination.numa.didserver.controller;

import com.managination.numa.didserver.dto.ErrorResponse;
import com.managination.numa.didserver.dto.ServerPublicKeyResponse;
import com.managination.numa.didserver.service.DidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public-key")
@Tag(name = "Server Configuration", description = "Server public key and configuration endpoints")
public class ServerConfigController {

    private final DidService didService;

    public ServerConfigController(DidService didService) {
        this.didService = didService;
    }

    @GetMapping
    @Operation(summary = "Get the server's public key", operationId = "getServerPublicKey")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Server public key",
            content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = ServerPublicKeyResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> getServerPublicKey() {
        try {
            ServerPublicKeyResponse response = didService.getServerPublicKey();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("internal_error", "Failed to retrieve server public key"));
        }
    }
}
