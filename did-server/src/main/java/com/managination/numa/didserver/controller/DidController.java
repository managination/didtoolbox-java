package com.managination.numa.didserver.controller;

import ch.admin.bj.swiyu.didtoolbox.model.WebVerifiableHistoryDidLogMetaPeeker;
import ch.admin.eid.didresolver.Did;
import com.managination.numa.didserver.dto.ErrorResponse;
import com.managination.numa.didserver.dto.HealthStatus;
import com.managination.numa.didserver.service.DidService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DidController {

    private static final Logger log = LoggerFactory.getLogger(DidController.class);

    private final DidService didService;

    public DidController(DidService didService) {
        this.didService = didService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthStatus> healthCheck() {
        HealthStatus health = didService.getHealthStatus();
        HttpStatus httpStatus = "UP".equals(health.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

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
            String normalizedLog = content.replaceAll("\\r?\\n\\s*", "");

            String id = WebVerifiableHistoryDidLogMetaPeeker.peek(normalizedLog).getDidDoc().getId();

            String extractedDomain = extractDomainFromDid(id);
            if (!extractedDomain.equals(domain)) {
                return ResponseEntity.badRequest()
                        .body("Domain mismatch: expected " + extractedDomain + " but got " + domain);
            }

            Path targetPath = getFilePath(id);

            if (Files.exists(targetPath)) {
                String existingContent = Files.readString(targetPath, StandardCharsets.UTF_8);
                String normalizedExisting = existingContent.replaceAll("\\r?\\n\\s*", "");
                String existingId = WebVerifiableHistoryDidLogMetaPeeker.peek(normalizedExisting).getDidDoc().getId();
                String existingScid = extractScidFromDid(existingId);

                String newScid = extractScidFromDid(id);

                if (!newScid.equals(existingScid)) {
                    return ResponseEntity.badRequest()
                            .body("SCID mismatch: existing DID has SCID " + existingScid + " but new DID has SCID " + newScid);
                }
            }

            new Did(id).resolveAll(normalizedLog);

            saveFile(targetPath, content);

            return ResponseEntity.ok("DID uploaded and verified successfully: " + id);
        } catch (Exception e) {
            log.error("Verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Verification FAILED: " + e.getMessage() + (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : ""));
        }
    }

    @GetMapping("/**")
    public ResponseEntity<?> downloadDid(HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI();

            if (requestPath.equals("/dids") || requestPath.equals("/health")) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }

            String domain = request.getServerName();
            String cleanPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;

            Path fullPath = Paths.get(System.getProperty("storage.did.path", "./storage/dids")).resolve(domain);

            if (cleanPath.isEmpty() || cleanPath.startsWith(".well-known")) {
                fullPath = fullPath.resolve(".well-known");
            } else {
            String[] parts = cleanPath.split("/");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    if (part.contains("..")) {
                        return ResponseEntity.badRequest().body("Invalid path: path traversal not allowed");
                    }
                    fullPath = fullPath.resolve(part);
                }
            }

            Path normalizedFull = fullPath.normalize();
            Path normalizedBase = Paths.get(System.getProperty("storage.did.path", "./storage/dids")).normalize();
            if (!normalizedFull.startsWith(normalizedBase)) {
                return ResponseEntity.badRequest().body("Invalid path: access outside storage directory");
            }
            }

            Path targetFile = fullPath.resolve("did.jsonl");

            if (!Files.exists(targetFile)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                      .body("DID file not found for this domain/path");
            }

            org.springframework.core.io.Resource resource = new UrlResource(targetFile.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/jsonl"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"did.jsonl\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading DID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String extractScidFromDid(String didId) {
        String[] parts = didId.split(":");
        if (parts.length < 4 || !parts[0].equals("did") || !parts[1].equals("webvh")) {
            throw new IllegalArgumentException("Only did:webvh with SCID is supported currently");
        }
        return parts[2];
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
        String domain = parts[3];
        Path fullPath = Paths.get(System.getProperty("storage.did.path", "./storage/dids")).resolve(domain);

        if (parts.length == 4) {
            fullPath = fullPath.resolve(".well-known");
        } else {
            for (int i = 4; i < parts.length; i++) {
                fullPath = fullPath.resolve(parts[i]);
            }
        }

        return fullPath.resolve("did.jsonl");
    }

    private void saveFile(Path targetPath, String content) throws java.io.IOException {
        Path parentDir = targetPath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(targetPath, content);
    }
}
