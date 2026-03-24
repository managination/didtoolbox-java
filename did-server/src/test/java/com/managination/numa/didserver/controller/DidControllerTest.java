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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testUploadInvalidDid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("didJsonl", "invalid.jsonl", "application/json", "invalid json".getBytes());

        mockMvc.perform(multipart("/dids")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Verification FAILED")));
    }

}
