package com.managination.numa.didserver.service;

import ch.admin.bj.swiyu.didtoolbox.model.DidLogMetaPeekerException;
import ch.admin.bj.swiyu.didtoolbox.model.WebVerifiableHistoryDidLogMetaPeeker;
import ch.admin.eid.didresolver.Did;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managination.numa.didserver.dto.*;
import com.managination.numa.didserver.model.DidDocument;
import com.managination.numa.didserver.model.JsonWebKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DidService {

    private static final Logger log = LoggerFactory.getLogger(DidService.class);

    @Value("${storage.did.path:./storage/dids}")
    private String storagePath = "./storage/dids";

    private final ConcurrentHashMap<String, DidDocument> didStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> didLogStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> didVersionStore = new ConcurrentHashMap<>();

    private KeyPair serverKeyPair;
    private JsonWebKey serverPublicKey;
    private Instant keyRotatedAt;

    public DidService() {
    }

    @PostConstruct
    public void init() {
        initializeServerKey();
    }

    private void initializeServerKey() {
        String path = storagePath != null ? storagePath : "./storage/dids";
        try {
            Path storageDir = Paths.get(path);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            Path privateKeyPath = storageDir.resolve("server-key.pem");
            Path publicKeyPath = storageDir.resolve("server-pubkey.pem");

            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                serverKeyPair = loadKeyPair(privateKeyPath, publicKeyPath);
            } else {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
                keyGen.initialize(256);
                serverKeyPair = keyGen.generateKeyPair();
                saveKeyPair(serverKeyPair, privateKeyPath, publicKeyPath);
            }

            ECPublicKey pubKey = (ECPublicKey) serverKeyPair.getPublic();

            byte[] xBytes = pubKey.getW().getAffineX().toByteArray();
            byte[] yBytes = pubKey.getW().getAffineY().toByteArray();

            serverPublicKey = new JsonWebKey(
                "EC",
                "P-256",
                Base64.getUrlEncoder().withoutPadding().encodeToString(xBytes),
                Base64.getUrlEncoder().withoutPadding().encodeToString(yBytes)
            );
            keyRotatedAt = Instant.now();
        } catch (Exception e) {
            log.error("Failed to initialize server key pair", e);
            throw new RuntimeException("Failed to initialize server key pair", e);
        }
    }

    private void saveKeyPair(KeyPair keyPair, Path privateKeyPath, Path publicKeyPath) throws IOException {
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded()) +
                "\n-----END PRIVATE KEY-----\n";

        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----\n";

        Files.writeString(privateKeyPath, privateKeyPem);
        Files.writeString(publicKeyPath, publicKeyPem);
    }

    private KeyPair loadKeyPair(Path privateKeyPath, Path publicKeyPath) throws Exception {
        String privateKeyPem = Files.readString(privateKeyPath, StandardCharsets.UTF_8);
        String publicKeyPem = Files.readString(publicKeyPath, StandardCharsets.UTF_8);

        byte[] privateKeyBytes = Base64.getMimeDecoder().decode(
                privateKeyPem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "")
        );

        byte[] publicKeyBytes = Base64.getMimeDecoder().decode(
                publicKeyPem
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "")
        );

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        return new KeyPair(publicKey, privateKey);
    }

    public HealthStatus getHealthStatus() {
        FilesystemHealth fsHealth = checkFilesystemHealth();
        MemoryHealth memHealth = checkMemoryHealth();
        JvmHealth jvmHealth = checkJvmHealth();
        EnvironmentHealth envHealth = checkEnvironmentHealth();

        boolean allHealthy = "UP".equals(fsHealth.status())
            && "UP".equals(memHealth.status());

        return new HealthStatus(
            allHealthy ? "UP" : "DEGRADED",
            Instant.now(),
            fsHealth,
            memHealth,
            jvmHealth,
            envHealth
        );
    }

    private FilesystemHealth checkFilesystemHealth() {
        try {
            Path resolvedPath = Paths.get(storagePath);
            if (!Files.exists(resolvedPath)) {
                Files.createDirectories(resolvedPath);
            }

            Path testFile = resolvedPath.resolve(".health-check-temp");
            Files.writeString(testFile, "health-check");
            Files.delete(testFile);

            if (!Files.isReadable(resolvedPath)) {
                return new FilesystemHealth("DOWN", storagePath, null, "Storage path is not readable");
            }

            long usableSpace = Files.getFileStore(resolvedPath).getUsableSpace();
            long minRequiredSpace = 100 * 1024 * 1024;

            if (usableSpace < minRequiredSpace) {
                return new FilesystemHealth("DEGRADED", storagePath, formatBytes(usableSpace), "Low disk space");
            }

            return new FilesystemHealth("UP", storagePath, formatBytes(usableSpace), null);
        } catch (Exception e) {
            return new FilesystemHealth("DOWN", storagePath, null, "Error: " + e.getMessage());
        }
    }

    private MemoryHealth checkMemoryHealth() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long heapMax = heapUsage.getMax();
        long heapUsed = heapUsage.getUsed();
        long heapCommitted = heapUsage.getCommitted();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;

        HeapMemory heap = new HeapMemory(
            formatBytes(heapMax),
            formatBytes(heapUsed),
            formatBytes(heapCommitted),
            String.format("%.2f", heapUsagePercent)
        );

        NonHeapMemory nonHeap = new NonHeapMemory(
            formatBytes(nonHeapUsage.getUsed()),
            formatBytes(nonHeapUsage.getCommitted())
        );

        String status;
        if (heapUsagePercent > 90) {
            status = "DOWN";
        } else if (heapUsagePercent > 75) {
            status = "DEGRADED";
        } else {
            status = "UP";
        }

        return new MemoryHealth(status, heap, nonHeap);
    }

    private JvmHealth checkJvmHealth() {
        Runtime runtime = Runtime.getRuntime();
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        int peakThreadCount = ManagementFactory.getThreadMXBean().getPeakThreadCount();
        long gcCollections = ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(bean -> bean.getCollectionCount())
            .sum();

        return new JvmHealth(
            runtime.availableProcessors(),
            threadCount,
            peakThreadCount,
            ManagementFactory.getRuntimeMXBean().getUptime() + "ms",
            ManagementFactory.getRuntimeMXBean().getVmName(),
            ManagementFactory.getRuntimeMXBean().getVmVersion(),
            (int) gcCollections,
            "UP"
        );
    }

    private EnvironmentHealth checkEnvironmentHealth() {
        String tempDirStatus;
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            tempDirStatus = Files.isWritable(tempDir) ? "writable" : "not writable";
        } catch (Exception e) {
            tempDirStatus = "error: " + e.getMessage();
        }

        return new EnvironmentHealth(
            tempDirStatus,
            System.getProperty("user.dir"),
            System.getProperty("file.encoding"),
            java.util.Locale.getDefault().toString(),
            "UP"
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "unknown";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DidRegistrationResponse registerDid(DidRegistrationRequest request) {
        if (request.did() == null || !request.did().startsWith("did:webvh:")) {
            throw new IllegalArgumentException("Invalid DID format. Must start with did:webvh:");
        }

        if (request.document() == null || request.document().id() == null) {
            throw new IllegalArgumentException("DID document is required");
        }

        if (request.log() == null || request.log().isEmpty()) {
            throw new IllegalArgumentException("DID log is required");
        }

        String did = request.did();

        if (didStore.containsKey(did)) {
            throw new IllegalStateException("DID already exists: " + did);
        }

        try {
            String logContent = request.log().stream()
                .map(entry -> {
                    try {
                        return objectMapper.writeValueAsString(entry);
                    } catch (Exception e) {
                        return "";
                    }
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

            String normalizedLog = logContent.replaceAll("\\r?\\n\\s*", "");
            new Did(did).resolveAll(normalizedLog);

            didStore.put(did, request.document());
            didLogStore.put(did, logContent);
            didVersionStore.put(did, "0");

            Path targetPath = getFilePath(did);
            saveFile(targetPath, logContent);

            return new DidRegistrationResponse(true, did, "DID registered successfully");
        } catch (Exception e) {
            log.error("Failed to register DID: {}", did, e);
            throw new RuntimeException("DID registration failed: " + e.getMessage(), e);
        }
    }

    public DidDocument resolveDid(String did) {
        if (!did.startsWith("did:webvh:")) {
            throw new IllegalArgumentException("Invalid DID format. Must start with did:webvh:");
        }

        DidDocument document = didStore.get(did);
        if (document == null) {
            throw new DidNotFoundException("DID not found: " + did);
        }

        return document;
    }

    public DidUpdateResponse updateDid(String did, DidUpdateRequest request) {
        if (!did.startsWith("did:webvh:")) {
            throw new IllegalArgumentException("Invalid DID format. Must start with did:webvh:");
        }

        String currentVersion = didVersionStore.get(did);
        if (currentVersion == null) {
            throw new DidNotFoundException("DID not found: " + did);
        }

        if (request.versionId() != null && !request.versionId().equals(currentVersion)) {
            throw new VersionConflictException(
                "Version conflict",
                currentVersion,
                request.versionId(),
                didStore.get(did)
            );
        }

        try {
            String existingLog = didLogStore.get(did);
            if (existingLog != null && !existingLog.isBlank()) {
                WebVerifiableHistoryDidLogMetaPeeker.peek(existingLog);
            }

            int newVersion = Integer.parseInt(currentVersion) + 1;
            String newVersionId = String.valueOf(newVersion);

            didStore.put(did, request.document());
            didVersionStore.put(did, newVersionId);

            if (request.logEntry() != null) {
                String newLogEntry = objectMapper.writeValueAsString(request.logEntry());
                String updatedLog = (existingLog != null && !existingLog.isBlank())
                    ? existingLog + "\n" + newLogEntry
                    : newLogEntry;
                didLogStore.put(did, updatedLog);

                Path targetPath = getFilePath(did);
                saveFile(targetPath, updatedLog);
            }

            return new DidUpdateResponse(true, newVersionId, "DID updated successfully");
        } catch (DidLogMetaPeekerException e) {
            throw new IllegalArgumentException("DID log verification failed: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid version ID format", e);
        } catch (Exception e) {
            throw new RuntimeException("DID update failed: " + e.getMessage(), e);
        }
    }

    public KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

    public String getIssuerDid() {
        return "did:webvh:SCID:issuer.did.ninja";
    }

    public ServerPublicKeyResponse getServerPublicKey() {
        return new ServerPublicKeyResponse(
            serverPublicKey,
            "server-key-1",
            "ES256",
            keyRotatedAt
        );
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
        Path fullPath = Paths.get(storagePath).resolve(domain);

        if (parts.length == 4) {
            fullPath = fullPath.resolve(".well-known");
        } else {
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

    public static class DidNotFoundException extends RuntimeException {
        public DidNotFoundException(String message) {
            super(message);
        }
    }

    public static class VersionConflictException extends RuntimeException {
        private final String serverVersionId;
        private final String clientVersionId;
        private final DidDocument serverDocument;

        public VersionConflictException(String message, String serverVersionId, String clientVersionId, DidDocument serverDocument) {
            super(message);
            this.serverVersionId = serverVersionId;
            this.clientVersionId = clientVersionId;
            this.serverDocument = serverDocument;
        }

        public String getServerVersionId() {
            return serverVersionId;
        }

        public String getClientVersionId() {
            return clientVersionId;
        }

        public DidDocument getServerDocument() {
            return serverDocument;
        }
    }
}
