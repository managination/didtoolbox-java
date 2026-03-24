package com.managination.numa.didserver.controller;

import ch.admin.bj.swiyu.didtoolbox.model.WebVerifiableHistoryDidLogMetaPeeker;
import ch.admin.eid.didresolver.Did;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DidController {

    private static final Logger log = LoggerFactory.getLogger(DidController.class);

    @Value("${storage.did.path}")
    private String storagePath;

    @PostMapping("/dids")
    public ResponseEntity<String> uploadDid(
            @RequestParam("didJsonl") MultipartFile file,
            HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            String domain = request.getServerName();
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            // Verification logic similar to com.managination.numa.Main
            String normalizedLog = content.replaceAll("\\r?\\n\\s*", "");

            String id = WebVerifiableHistoryDidLogMetaPeeker.peek(normalizedLog).getDidDoc().getId();

            // Extract domain from ID and verify it matches the provided domain
            String extractedDomain = extractDomainFromDid(id);
            if (!extractedDomain.equals(domain)) {
                return ResponseEntity.badRequest()
                        .body("Domain mismatch: expected " + extractedDomain + " but got " + domain);
            }

            new Did(id).resolveAll(normalizedLog);

            Path targetPath = getFilePath(id);

            // If successful, store the file
            saveFile(targetPath, content);

            return ResponseEntity.ok("DID uploaded and verified successfully: " + id);
        } catch (Exception e) {
            log.error("Verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Verification FAILED: " + e.getMessage() + (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : ""));
        }
    }

    private String extractDomainFromDid(String didId) {
        String[] parts = didId.split(":");
        if (parts.length < 4 || !parts[0].equals("did") || !parts[1].equals("webvh")) {
            throw new IllegalArgumentException("Only did:webvh with SCID is supported currently");
        }
        return parts[3];
    }

    private Path getFilePath(String didId) {
        String[] parts = didId.split(":");
        if (parts.length < 4 || !parts[0].equals("did") || !parts[1].equals("webvh")) {
            throw new IllegalArgumentException("Only did:webvh with SCID is supported currently");
        }

        // did:webvh:SCID:domain[:path1:path2...]
        // parts[0] = did
        // parts[1] = webvh
        // parts[2] = SCID
        // parts[3] = Domain
        // parts[4..] = Path components

        String domain = parts[3];
        Path fullPath = Paths.get(storagePath).resolve(domain);

        if (parts.length == 4) {
            // No path, use .well-known
            fullPath = fullPath.resolve(".well-known");
        } else {
            // Path exists
            for (int i = 4; i < parts.length; i++) {
                fullPath = fullPath.resolve(parts[i]);
            }
        }

        return fullPath.resolve("did.jsonl");
    }

    private void saveFile(Path targetPath, String content) throws IOException {
        Path parentDir = targetPath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(targetPath, content);
    }
}
