package com.managination.numa.didserver.controller;

import com.managination.numa.didserver.dto.*;
import com.managination.numa.didserver.service.PresentationVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/presentations")
@Tag(name = "Verifiable Presentations", description = "Endpoints for submitting and verifying verifiable presentations")
public class PresentationController {

    private final PresentationVerificationService presentationVerificationService;

    public PresentationController(PresentationVerificationService presentationVerificationService) {
        this.presentationVerificationService = presentationVerificationService;
    }

    @PostMapping
    @Operation(summary = "Submit a verifiable presentation for verification", operationId = "submitPresentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presentation verified successfully",
            content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = PresentationVerificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid presentation",
            content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Presentation verification failed",
            content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = PresentationVerificationResponse.class)))
    })
    public ResponseEntity<?> submitPresentation(@RequestBody PresentationSubmissionRequest request) {
        if (request.presentation() == null || request.presentation().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", "presentation is required"));
        }

        PresentationVerificationResponse response = presentationVerificationService.verifyPresentation(request.presentation());

        if (response.verified()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }
}
