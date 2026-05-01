package com.managination.numa.didserver.exception;

import com.managination.numa.didserver.dto.ErrorResponse;
import com.managination.numa.didserver.service.CredentialOfferStore;
import com.managination.numa.didserver.service.DidService;
import com.managination.numa.didserver.service.RewardService;
import com.managination.numa.didserver.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DidService.DidNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDidNotFound(DidService.DidNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(DidService.VersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(DidService.VersionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("version_conflict", ex.getMessage()));
    }

    @ExceptionHandler(SessionService.SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionService.SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(RewardService.RewardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRewardNotFound(RewardService.RewardNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(CredentialOfferStore.InvalidPreAuthorizedCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCode(CredentialOfferStore.InvalidPreAuthorizedCodeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("invalid_grant", ex.getMessage()));
    }

    @ExceptionHandler(CredentialOfferStore.ExpiredPreAuthorizedCodeException.class)
    public ResponseEntity<ErrorResponse> handleExpiredCode(CredentialOfferStore.ExpiredPreAuthorizedCodeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("invalid_grant", ex.getMessage()));
    }

    @ExceptionHandler(CredentialOfferStore.AlreadyConsumedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyConsumed(CredentialOfferStore.AlreadyConsumedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("invalid_grant", ex.getMessage()));
    }

    @ExceptionHandler(CredentialOfferStore.InvalidPinException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPin(CredentialOfferStore.InvalidPinException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("invalid_pin", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("invalid_request", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
