package com.managination.numa;

import ch.admin.bj.swiyu.didtoolbox.context.DidLogCreatorContext;
import ch.admin.bj.swiyu.didtoolbox.model.VerificationMethod;
import ch.admin.bj.swiyu.didtoolbox.vc_data_integrity.EdDsaJcs2022VcDataIntegrityCryptographicSuite;
import ch.admin.bj.swiyu.didtoolbox.JwkUtils;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class NumaCreate {

    public static void main(String... args) {
        if (args.length < 1) {
            System.err.println("Usage: java com.managination.numa.NumaCreate <didDomain>");
            System.exit(1);
        }

        String didDomain = args[0];
        try {
            // Initialize a cryptographic suite with a newly generated Ed25519 key pair for the proof
            var cryptoSuite = new EdDsaJcs2022VcDataIntegrityCryptographicSuite();

            // Create target directory structure: did-operations/{domain}/.didtoolbox
            Path baseDir = Path.of("did-operations", didDomain);
            Path dotDidToolboxDir = baseDir.resolve(".didtoolbox");
            Files.createDirectories(dotDidToolboxDir);

            // Paths for keys and DID log
            Path publicPem = dotDidToolboxDir.resolve("public.pem");
            Path privatePem = dotDidToolboxDir.resolve("private.pem");
            Path authEcPem = dotDidToolboxDir.resolve("auth-ec.pem");
            Path assertEcPem = dotDidToolboxDir.resolve("assert-ec.pem");
            Path didLogPath = baseDir.resolve("did.jsonl");
            Path didLogCopyPath = dotDidToolboxDir.resolve("did.jsonl");

            // Write Ed25519 keys (used for the proof) to .didtoolbox
            cryptoSuite.writePublicKeyPemFile(publicPem);
            cryptoSuite.writePkcs8PemFile(privatePem);

            // Generate P-256 keys (used for verification methods in DID document)
            // JwkUtils.generatePublicEC256 writes both private/public to the file and public only to .pub
            JwkUtils.generatePublicEC256("auth-0", authEcPem.toFile(), true);
            JwkUtils.generatePublicEC256("assert-0", assertEcPem.toFile(), true);

            // Construct the identifier registry URL based on the specified domain
            String urlStr = didDomain.startsWith("http") ? didDomain : "https://" + didDomain;
            URL identifierRegistryUrl = URI.create(urlStr).toURL();

            // Create the initial DID log entry
            // We use P-256 public keys for the verification methods because the library's
            // validator currently requires P-256 JWKs (with 'y' property).
            String didLog = DidLogCreatorContext.builder()
                    .cryptographicSuite(cryptoSuite)
                    .assertionMethods(Set.of(VerificationMethod.of("assert-0", Path.of(assertEcPem + ".pub"))))
                    .authentications(Set.of(VerificationMethod.of("auth-0", Path.of(authEcPem + ".pub"))))
                    .build()
                    .create(identifierRegistryUrl);

            // Save DID log to both locations
            Files.writeString(didLogPath, didLog);
            Files.writeString(didLogCopyPath, didLog);

            // Output the newly created DID log
            System.out.println("DID Log created and stored in " + didLogPath);
            System.out.println(didLog);

        } catch (Exception e) {
            System.err.println("Error creating DID document: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
