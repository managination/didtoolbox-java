package com.managination.numa.didserver.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CredentialIssuanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRequestToken() throws Exception {
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code")
                .param("pre-authorized_code", "test-code")
                .param("user_pin", "123456"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testRequestTokenWrongGrantType() throws Exception {
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("pre-authorized_code", "test-code"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    void testRequestTokenMissingCode() throws Exception {
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code")
                .param("pre-authorized_code", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void testRequestCredential() throws Exception {
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid-token")
                .content("{\"format\":\"jwt_vc_json\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testRequestCredentialNoAuth() throws Exception {
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"format\":\"jwt_vc_json\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void testRequestCredentialInvalidToken() throws Exception {
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid-token")
                .content("{\"format\":\"jwt_vc_json\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"));
    }
}
