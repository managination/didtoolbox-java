package com.managination.numa.didserver.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@SpringBootTest
@AutoConfigureMockMvc
public class DidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testUploadValidDid() throws Exception {
        // Path to the example DID log from the issue description context
        Path logPath = Paths.get("../did-operations/identifier-reg-api.trust-infra.swiyu-int.admin.ch/did.jsonl");
        byte[] content = Files.readAllBytes(logPath);

        // Clean up potential existing file to ensure success in isolation
        Path expectedPath = Paths.get("./storage/dids")
                .resolve("identifier-reg-api.trust-infra.swiyu-int.admin.ch")
                .resolve("api")
                .resolve("v1")
                .resolve("did")
                .resolve("9131093b-ef52-46b4-8626-76dd9fe7f345")
                .resolve("did.jsonl");
        Files.deleteIfExists(expectedPath);

        MockMultipartFile file = new MockMultipartFile("didJsonl", "didlog.jsonl", "application/json", content);

        mockMvc.perform(multipart("/dids")
                        .file(file)
                        .header("Host", "identifier-reg-api.trust-infra.swiyu-int.admin.ch"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DID uploaded and verified successfully")));

        // Verify file is stored
        assertTrue(Files.exists(expectedPath), "Expected file does not exist at: " + expectedPath.toAbsolutePath());
    }

    @Test
    public void testUploadRootDomainDid() throws Exception {
        // Path to the micha.did.ninja DID log
        Path logPath = Paths.get("../did-operations/micha.did.ninja/did.jsonl");
        byte[] content = Files.readAllBytes(logPath);

        // Clean up potential existing file to ensure success in isolation
        Path expectedPath = Paths.get("./storage/dids")
                .resolve("micha.did.ninja")
                .resolve(".well-known")
                .resolve("did.jsonl");
        Files.deleteIfExists(expectedPath);

        MockMultipartFile file = new MockMultipartFile("didJsonl", "did.jsonl", "application/json", content);

        mockMvc.perform(multipart("/dids")
                        .file(file)
                        .header("Host", "micha.did.ninja"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DID uploaded and verified successfully")));

        // Verify file is stored in .well-known
        assertTrue(Files.exists(expectedPath), "Expected file does not exist at: " + expectedPath.toAbsolutePath());
    }

    @Test
    public void testUploadDomainMismatch() throws Exception {
        Path logPath = Paths.get("../did-operations/micha.did.ninja/did.jsonl");
        byte[] content = Files.readAllBytes(logPath);

        MockMultipartFile file = new MockMultipartFile("didJsonl", "did.jsonl", "application/json", content);

        mockMvc.perform(multipart("/dids")
                        .file(file)
                        .header("Host", "wrong.domain.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Domain mismatch")));
    }

    @Test
    public void testScidCheck() throws Exception {
        // 1. Upload initial DID
        Path logPath = Paths.get("../did-operations/micha.did.ninja/did.jsonl");
        byte[] content = Files.readAllBytes(logPath);
        MockMultipartFile file1 = new MockMultipartFile("didJsonl", "did.jsonl", "application/json", content);

        Path storagePath = Paths.get("./storage/dids")
                .resolve("micha.did.ninja")
                .resolve(".well-known")
                .resolve("did.jsonl");
        Files.deleteIfExists(storagePath);

        mockMvc.perform(multipart("/dids")
                        .file(file1)
                        .header("Host", "micha.did.ninja"))
                .andExpect(status().isOk());

        // 2. Try to upload a DID with SAME domain but DIFFERENT SCID
        // We use the other log we have (identifier-reg-api...) but force Host to 'micha.did.ninja'
        // This will trigger SCID mismatch IF it passes Domain Mismatch.
        // Wait, the current code checks Domain Mismatch FIRST.
        /*
            String extractedDomain = extractDomainFromDid(id);
            if (!extractedDomain.equals(domain)) { ... }
         */
        // So we need a log that HAS micha.did.ninja as domain but different SCID.
        // Or we change the code to check SCID first? No, domain check is also important.

        // Let's use a trick: in the test, we can use a log that has the same domain but we'll have to bypass resolveAll().
        // Actually, if we want to test SCID mismatch, we can just use any valid log and set the Host header to match THAT log's domain,
        // BUT we need the file to ALREADY EXIST at that path with a DIFFERENT SCID.

        Path otherLogPath = Paths.get("../did-operations/identifier-reg-api.trust-infra.swiyu-int.admin.ch/did.jsonl");
        byte[] otherContent = Files.readAllBytes(otherLogPath);
        MockMultipartFile file2 = new MockMultipartFile("didJsonl", "did.jsonl", "application/json", otherContent);

        // We need to place file1 (micha) at the path where file2 (identifier-reg) would be stored.
        // Identifier-reg path: storage/dids/identifier-reg.../api/v1/did/9131093b.../did.jsonl
        Path targetPathForOther = Paths.get("./storage/dids")
                .resolve("identifier-reg-api.trust-infra.swiyu-int.admin.ch")
                .resolve("api")
                .resolve("v1")
                .resolve("did")
                .resolve("9131093b-ef52-46b4-8626-76dd9fe7f345")
                .resolve("did.jsonl");

        Files.createDirectories(targetPathForOther.getParent());
        Files.write(targetPathForOther, content); // Write MICHA log to IDENTIFIER-REG path

        mockMvc.perform(multipart("/dids")
                        .file(file2)
                        .header("Host", "identifier-reg-api.trust-infra.swiyu-int.admin.ch"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SCID mismatch")));
    }

    @Test
    public void testUploadInvalidDid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("didJsonl", "invalid.jsonl", "application/json", "invalid json".getBytes());

        mockMvc.perform(multipart("/dids")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Verification FAILED")));
    }

    @Test
    public void testDownloadDid() throws Exception {
        // 1. Setup a file to download
        String domain = "example.com";
        Path storageDir = Paths.get("./storage/dids").resolve(domain).resolve(".well-known");
        Files.createDirectories(storageDir);
        Path didFile = storageDir.resolve("did.jsonl");
        String content = "test content";
        Files.writeString(didFile, content);

        // 2. Test download from .well-known
        mockMvc.perform(get("/.well-known")
                        .header("Host", domain))
                .andExpect(status().isOk())
                .andExpect(content().string(content))
                .andExpect(header().string("Content-Type", "application/jsonl"));

        // 3. Test download from root
        mockMvc.perform(get("/")
                        .header("Host", domain))
                .andExpect(status().isOk())
                .andExpect(content().string(content));

        // 4. Test download from subpath
        Path subDir = Paths.get("./storage/dids").resolve(domain).resolve("sub").resolve("path");
        Files.createDirectories(subDir);
        Path subDidFile = subDir.resolve("did.jsonl");
        String subContent = "sub content";
        Files.writeString(subDidFile, subContent);

        mockMvc.perform(get("/sub/path")
                        .header("Host", domain))
                .andExpect(status().isOk())
                .andExpect(content().string(subContent));

        // 5. Test not found
        mockMvc.perform(get("/non/existent")
                        .header("Host", domain))
                .andExpect(status().isNotFound());

        // 6. Test GET /dids should be 405 (since it's only POST)
        mockMvc.perform(get("/dids"))
                .andExpect(status().isMethodNotAllowed());
    }

}
