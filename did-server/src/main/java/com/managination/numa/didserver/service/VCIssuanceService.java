package com.managination.numa.didserver.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VCIssuanceService {

    private final DidService didService;

    public VCIssuanceService(DidService didService) {
        this.didService = didService;
    }

    public String createCredential(String holderDid, String credentialType, String issuerDid) {
        try {
            KeyPair keyPair = didService.getServerKeyPair();
            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

            ECKey ecKey = new ECKey.Builder(Curve.P_256, publicKey)
                .privateKey(privateKey)
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .build();

            long now = System.currentTimeMillis() / 1000;
            long oneYear = now + (365L * 24 * 60 * 60);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuerDid)
                .subject(holderDid)
                .claim("vc", Map.of(
                    "@context", List.of("https://www.w3.org/2018/credentials/v1"),
                    "type", List.of("VerifiableCredential", credentialType),
                    "credentialSubject", Map.of("id", holderDid)
                ))
                .issueTime(new Date(now * 1000))
                .expirationTime(new Date(oneYear * 1000))
                .notBeforeTime(new Date(now * 1000))
                .jwtID(UUID.randomUUID().toString())
                .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(issuerDid + "#key-1")
                .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new ECDSASigner(ecKey));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign credential", e);
        }
    }
}
