package com.managination.numa.didserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialOfferStoreTest {

    private CredentialOfferStore store;

    @BeforeEach
    void setUp() {
        store = new CredentialOfferStore();
    }

    @Test
    void createOffer_returnsNonEmptyCode() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 3600);
        assertNotNull(code);
        assertFalse(code.isBlank());
    }

    @Test
    void createOffer_returnsUniqueCodes() {
        String code1 = store.createOffer("did:webvh:SCID:holder1.example.com", "EmployeeCredential", false, null, 3600);
        String code2 = store.createOffer("did:webvh:SCID:holder2.example.com", "EmployeeCredential", false, null, 3600);
        assertNotEquals(code1, code2);
    }

    @Test
    void validateAndConsume_returnsOfferForValidCode() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 3600);
        CredentialOfferStore.CredentialOffer offer = store.validateAndConsume(code, null);
        assertNotNull(offer);
        assertEquals(code, offer.preAuthorizedCode());
        assertEquals("did:webvh:SCID:holder.example.com", offer.holderDid());
        assertEquals("EmployeeCredential", offer.credentialType());
        assertTrue(offer.consumed());
    }

    @Test
    void validateAndConsume_marksOfferAsConsumed() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 3600);
        store.validateAndConsume(code, null);
        assertThrows(CredentialOfferStore.AlreadyConsumedException.class, () -> store.validateAndConsume(code, null));
    }

    @Test
    void validateAndConsume_throwsForInvalidCode() {
        assertThrows(CredentialOfferStore.InvalidPreAuthorizedCodeException.class,
            () -> store.validateAndConsume("nonexistent-code", null));
    }

    @Test
    void validateAndConsume_validatesPinWhenRequired() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", true, "1234", 3600);
        CredentialOfferStore.CredentialOffer offer = store.validateAndConsume(code, "1234");
        assertNotNull(offer);
        assertTrue(offer.consumed());
    }

    @Test
    void validateAndConsume_throwsForWrongPin() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", true, "1234", 3600);
        assertThrows(CredentialOfferStore.InvalidPinException.class,
            () -> store.validateAndConsume(code, "5678"));
    }

    @Test
    void validateAndConsume_throwsForMissingPinWhenRequired() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", true, "1234", 3600);
        assertThrows(CredentialOfferStore.InvalidPinException.class,
            () -> store.validateAndConsume(code, null));
    }

    @Test
    void validateAndConsume_throwsForExpiredCode() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 0);
        assertThrows(CredentialOfferStore.ExpiredPreAuthorizedCodeException.class,
            () -> store.validateAndConsume(code, null));
    }

    @Test
    void getOfferByCode_returnsNullForUnknownCode() {
        assertNull(store.getOfferByCode("unknown-code"));
    }

    @Test
    void getOfferByCode_returnsUnconsumedOffer() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 3600);
        CredentialOfferStore.CredentialOffer offer = store.getOfferByCode(code);
        assertNotNull(offer);
        assertFalse(offer.consumed());
    }

    @Test
    void cleanupExpired_removesExpiredEntries() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 0);
        assertNotNull(store.getOfferByCode(code));
        store.cleanupExpired();
        assertNull(store.getOfferByCode(code));
    }

    @Test
    void cleanupExpired_keepsValidEntries() {
        String code = store.createOffer("did:webvh:SCID:holder.example.com", "EmployeeCredential", false, null, 3600);
        store.cleanupExpired();
        assertNotNull(store.getOfferByCode(code));
    }
}
