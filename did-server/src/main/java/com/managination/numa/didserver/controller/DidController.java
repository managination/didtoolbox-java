package com.managination.numa.didserver.controller;

import ch.admin.bj.swiyu.didtoolbox.model.WebVerifiableHistoryDidLogMetaPeeker;
import ch.admin.eid.didresolver.Did;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DidController {

    private static final Logger log = LoggerFactory.getLogger(DidController.class);

    @Value("${storage.did.path}")
    private String storagePath;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());

        boolean allHealthy = true;

        // Filesystem health check
        Map<String, Object> filesystemHealth = checkFilesystemHealth();
        health.put("filesystem", filesystemHealth);
        if (!"UP".equals(filesystemHealth.get("status"))) {
            allHealthy = false;
        }

        // Memory health check
        Map<String, Object> memoryHealth = checkMemoryHealth();
        health.put("memory", memoryHealth);
        if (!"UP".equals(memoryHealth.get("status"))) {
            allHealthy = false;
        }

        // JVM health check
        Map<String, Object> jvmHealth = checkJvmHealth();
        health.put("jvm", jvmHealth);

        // Environment check
        Map<String, Object> envHealth = checkEnvironmentHealth();
        health.put("environment", envHealth);

        health.put("status", allHealthy ? "UP" : "DEGRADED");

        HttpStatus httpStatus = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

    private Map<String, Object> checkFilesystemHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            Path storageRoot = Paths.get(storagePath).getRoot();

            // Check if we can resolve the storage path
            Path resolvedPath = Paths.get(storagePath);

            // Create directory if it doesn't exist and check writability
            if (!Files.exists(resolvedPath)) {
                Files.createDirectories(resolvedPath);
            }

            // Test write access by creating and deleting a temp file
            Path testFile = resolvedPath.resolve(".health-check-temp");
            Files.writeString(testFile, "health-check");
            Files.delete(testFile);

            // Check if the path is readable
            if (!Files.isReadable(resolvedPath)) {
                result.put("status", "DOWN");
                result.put("reason", "Storage path is not readable");
                return result;
            }

            // Check available space (at least 100MB should be available)
            long usableSpace = Files.getFileStore(resolvedPath).getUsableSpace();
            long minRequiredSpace = 100 * 1024 * 1024; // 100MB

            if (usableSpace < minRequiredSpace) {
                result.put("status", "DEGRADED");
                result.put("reason", "Low disk space");
                result.put("availableSpace", formatBytes(usableSpace));
                return result;
            }

            result.put("status", "UP");
            result.put("storagePath", storagePath);
            result.put("availableSpace", formatBytes(usableSpace));

        } catch (SecurityException e) {
            result.put("status", "DOWN");
            result.put("reason", "Permission denied accessing storage path: " + e.getMessage());
        } catch (IOException e) {
            result.put("status", "DOWN");
            result.put("reason", "IO error accessing storage path: " + e.getMessage());
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("reason", "Unexpected error: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> result = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long heapMax = heapUsage.getMax();
        long heapUsed = heapUsage.getUsed();
        long heapCommitted = heapUsage.getCommitted();

        double heapUsagePercent = (double) heapUsed / heapMax * 100;

        result.put("heap", Map.of(
            "max", formatBytes(heapMax),
            "used", formatBytes(heapUsed),
            "committed", formatBytes(heapCommitted),
            "usagePercent", String.format("%.2f", heapUsagePercent)
        ));

        result.put("nonHeap", Map.of(
            "used", formatBytes(nonHeapUsage.getUsed()),
            "committed", formatBytes(nonHeapUsage.getCommitted())
        ));

        // Memory is considered unhealthy if heap usage exceeds 90%
        if (heapUsagePercent > 90) {
            result.put("status", "DOWN");
            result.put("reason", "Heap memory usage critically high: " + String.format("%.2f", heapUsagePercent) + "%");
        } else if (heapUsagePercent > 75) {
            result.put("status", "DEGRADED");
            result.put("reason", "Heap memory usage high: " + String.format("%.2f", heapUsagePercent) + "%");
        } else {
            result.put("status", "UP");
        }

        return result;
    }

    private Map<String, Object> checkJvmHealth() {
        Map<String, Object> result = new HashMap<>();

        Runtime runtime = Runtime.getRuntime();
        int availableProcessors = runtime.availableProcessors();

        // Get thread count
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        int peakThreadCount = ManagementFactory.getThreadMXBean().getPeakThreadCount();

        result.put("availableProcessors", availableProcessors);
        result.put("threadCount", threadCount);
        result.put("peakThreadCount", peakThreadCount);
        result.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() + "ms");
        result.put("vmName", ManagementFactory.getRuntimeMXBean().getVmName());
        result.put("vmVersion", ManagementFactory.getRuntimeMXBean().getVmVersion());

        // Check for GC pressure - iterate through GC MXBeans
        long gcCollections = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(bean -> bean.getCollectionCount() > 0 ? bean.getCollectionCount() : 0)
                .sum();
        result.put("gcCollections", gcCollections);

        result.put("status", "UP");
        return result;
    }

    private Map<String, Object> checkEnvironmentHealth() {
        Map<String, Object> result = new HashMap<>();

        // Check temp directory accessibility
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            if (Files.isWritable(tempDir)) {
                result.put("tempDir", "writable");
            } else {
                result.put("tempDir", "not writable");
            }
        } catch (Exception e) {
            result.put("tempDir", "error: " + e.getMessage());
        }

        // Check user dir
        try {
            String userDir = System.getProperty("user.dir");
            result.put("userDir", userDir);
        } catch (Exception e) {
            result.put("userDir", "error: " + e.getMessage());
        }

        // Check file encoding
        result.put("fileEncoding", System.getProperty("file.encoding"));
        result.put("defaultLocale", java.util.Locale.getDefault().toString());

        result.put("status", "UP");
        return result;
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "unknown";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
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
            // Verification logic similar to com.managination.numa.Main
            String normalizedLog = content.replaceAll("\\r?\\n\\s*", "");

            String id = WebVerifiableHistoryDidLogMetaPeeker.peek(normalizedLog).getDidDoc().getId();

            // Extract domain from ID and verify it matches the provided domain
            String extractedDomain = extractDomainFromDid(id);
            if (!extractedDomain.equals(domain)) {
                return ResponseEntity.badRequest()
                        .body("Domain mismatch: expected " + extractedDomain + " but got " + domain);
            }

            Path targetPath = getFilePath(id);

            // SCID check: if a did.jsonl already exists, ensure the new file has the same SCID
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

            // If successful, store the file
            saveFile(targetPath, content);

            return ResponseEntity.ok("DID uploaded and verified successfully: " + id);
        } catch (Exception e) {
            log.error("Verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Verification FAILED: " + e.getMessage() + (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : ""));
        }
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> downloadDid(HttpServletRequest request) {
        try {
            String domain = request.getServerName();
            String requestPath = request.getRequestURI();

            if (requestPath.equals("/dids") || requestPath.equals("/health")) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }

            // Remove leading / to get path components
            String cleanPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;

            Path fullPath = Paths.get(storagePath).resolve(domain);

            if (cleanPath.isEmpty() || cleanPath.equals(".well-known")) {
                fullPath = fullPath.resolve(".well-known");
            } else {
                String[] parts = cleanPath.split("/");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        fullPath = fullPath.resolve(part);
                    }
                }
            }

            Path targetFile = fullPath.resolve("did.jsonl");
            log.info("Downloading DID file: {}", targetFile);

            if (!Files.exists(targetFile)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(targetFile.toUri());
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
