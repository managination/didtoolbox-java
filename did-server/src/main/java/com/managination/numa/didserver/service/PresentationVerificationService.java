package com.managination.numa.didserver.service;

import com.managination.numa.didserver.dto.PresentationVerificationResponse;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PresentationVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PresentationVerificationService.class);

    public PresentationVerificationResponse verifyPresentation(String presentationJwt) {
        List<String> errors = new ArrayList<>();
        String holderDid = null;
        List<PresentationVerificationResponse.CredentialInfo> credentials = null;

        try {
            SignedJWT signedJwt = SignedJWT.parse(presentationJwt);

            String algorithm = signedJwt.getHeader().getAlgorithm().getName();
            Object publicKey = extractPublicKey(signedJwt);

            if (publicKey != null) {
                JWSVerifier verifier = createVerifier(algorithm, publicKey);
                if (verifier == null) {
                    errors.add("Unsupported algorithm: " + algorithm);
                } else if (!signedJwt.verify(verifier)) {
                    errors.add("JWT signature verification failed");
                }
            } else {
                errors.add("Unable to extract public key from JWT header");
            }

            holderDid = signedJwt.getJWTClaimsSet().getIssuer();

            java.util.Date expirationTime = signedJwt.getJWTClaimsSet().getExpirationTime();
            if (expirationTime != null && Instant.now().isAfter(expirationTime.toInstant())) {
                errors.add("Presentation has expired");
            }

            Object vpClaim = signedJwt.getJWTClaimsSet().getClaim("vp");
            if (vpClaim instanceof Map<?, ?> vpMap) {
                credentials = extractCredentialInfo(vpMap);
            } else {
                errors.add("Missing or invalid vp claim");
            }

        } catch (ParseException e) {
            errors.add("Invalid JWT format: " + e.getMessage());
        } catch (JOSEException e) {
            errors.add("JOSE processing error: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Verification error: " + e.getMessage());
        }

        return new PresentationVerificationResponse(
            errors.isEmpty(),
            errors.isEmpty() ? List.of() : List.copyOf(errors),
            holderDid,
            credentials
        );
    }

    private Object extractPublicKey(SignedJWT signedJwt) {
        return signedJwt.getHeader().getJWK();
    }

    private JWSVerifier createVerifier(String algorithm, Object publicKey) throws JOSEException {
        if (publicKey instanceof ECKey ecKey) {
            return new ECDSAVerifier(ecKey);
        } else if (publicKey instanceof OctetKeyPair okp) {
            return new Ed25519Verifier(okp);
        }
        return null;
    }

    private List<PresentationVerificationResponse.CredentialInfo> extractCredentialInfo(Map<?, ?> vpMap) {
        Object verifiableCredential = vpMap.get("verifiableCredential");
        if (verifiableCredential instanceof List<?> vcList) {
            List<PresentationVerificationResponse.CredentialInfo> result = new ArrayList<>();
            for (Object vc : vcList) {
                if (vc instanceof String vcJwt) {
                    try {
                        SignedJWT signedVc = SignedJWT.parse(vcJwt);
                        String issuer = signedVc.getJWTClaimsSet().getIssuer();
                        Object typeClaim = signedVc.getJWTClaimsSet().getClaim("vc");
                        List<String> types = List.of("VerifiableCredential");
                        if (typeClaim instanceof Map<?, ?> vcInner) {
                            Object typeObj = vcInner.get("type");
                            if (typeObj instanceof String s) {
                                types = List.of(s);
                            } else if (typeObj instanceof List<?> typeList) {
                                types = typeList.stream().map(Object::toString).toList();
                            }
                        }
                        result.add(new PresentationVerificationResponse.CredentialInfo(issuer, types, true));
                    } catch (ParseException e) {
                        result.add(new PresentationVerificationResponse.CredentialInfo("unknown", List.of("VerifiableCredential"), false));
                    }
                } else if (vc instanceof Map<?, ?> vcMap) {
                    String issuer = (String) vcMap.get("issuer");
                    Object typeObj = vcMap.get("type");
                    List<String> types;
                    if (typeObj instanceof String s) {
                        types = List.of(s);
                    } else if (typeObj instanceof List<?> typeList) {
                        types = typeList.stream().map(Object::toString).toList();
                    } else {
                        types = List.of("VerifiableCredential");
                    }
                    result.add(new PresentationVerificationResponse.CredentialInfo(issuer, types, true));
                }
            }
            return result.isEmpty() ? null : Collections.unmodifiableList(result);
        }
        return null;
    }
}
