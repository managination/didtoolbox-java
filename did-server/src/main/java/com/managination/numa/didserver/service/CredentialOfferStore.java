package com.managination.numa.didserver.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CredentialOfferStore {

    public record CredentialOffer(
        String preAuthorizedCode,
        String holderDid,
        String credentialType,
        boolean userPinRequired,
        String userPin,
        String cNonce,
        long cNonceExpiresAt,
        long createdAt,
        long expiresAt,
        boolean consumed,
        String sessionId
    ) {}

    private final ConcurrentHashMap<String, CredentialOffer> store = new ConcurrentHashMap<>();

    public String createOffer(String holderDid, String credentialType, boolean userPinRequired, String userPin, long ttlSeconds) {
        String preAuthorizedCode = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        String cNonce = UUID.randomUUID().toString();

        CredentialOffer offer = new CredentialOffer(
            preAuthorizedCode,
            holderDid,
            credentialType,
            userPinRequired,
            userPin,
            cNonce,
            now + (ttlSeconds * 1000L),
            now,
            now + (ttlSeconds * 1000L),
            false,
            null
        );

        store.put(preAuthorizedCode, offer);
        return preAuthorizedCode;
    }

    public CredentialOffer validateAndConsume(String preAuthorizedCode, String userPin) {
        CredentialOffer offer = store.get(preAuthorizedCode);

        if (offer == null) {
            throw new InvalidPreAuthorizedCodeException("Invalid pre-authorized code");
        }

        long now = System.currentTimeMillis();
        if (now >= offer.expiresAt()) {
            throw new ExpiredPreAuthorizedCodeException("Pre-authorized code has expired");
        }

        if (offer.consumed()) {
            throw new AlreadyConsumedException("Pre-authorized code has already been consumed");
        }

        if (offer.userPinRequired()) {
            if (userPin == null || !userPin.equals(offer.userPin())) {
                throw new InvalidPinException("Invalid or missing user PIN");
            }
        }

        CredentialOffer consumedOffer = new CredentialOffer(
            offer.preAuthorizedCode(),
            offer.holderDid(),
            offer.credentialType(),
            offer.userPinRequired(),
            offer.userPin(),
            offer.cNonce(),
            offer.cNonceExpiresAt(),
            offer.createdAt(),
            offer.expiresAt(),
            true,
            offer.sessionId()
        );

        store.put(preAuthorizedCode, consumedOffer);
        return consumedOffer;
    }

    public CredentialOffer getOfferByCode(String preAuthorizedCode) {
        return store.get(preAuthorizedCode);
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
    }

    public static class InvalidPreAuthorizedCodeException extends RuntimeException {
        public InvalidPreAuthorizedCodeException(String message) {
            super(message);
        }
    }

    public static class ExpiredPreAuthorizedCodeException extends RuntimeException {
        public ExpiredPreAuthorizedCodeException(String message) {
            super(message);
        }
    }

    public static class AlreadyConsumedException extends RuntimeException {
        public AlreadyConsumedException(String message) {
            super(message);
        }
    }

    public static class InvalidPinException extends RuntimeException {
        public InvalidPinException(String message) {
            super(message);
        }
    }
}
