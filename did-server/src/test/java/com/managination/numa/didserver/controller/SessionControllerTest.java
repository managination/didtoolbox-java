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
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreateSession() throws Exception {
        mockMvc.perform(post("/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transportType\":\"websocket\",\"credentialType\":\"jwt_vc_json\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.wsUrl").isNotEmpty());
    }

    @Test
    void testCreateSessionMissingTransportType() throws Exception {
        mockMvc.perform(post("/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentialType\":\"jwt_vc_json\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSessionStatus() throws Exception {
        String response = mockMvc.perform(post("/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transportType\":\"websocket\",\"credentialType\":\"jwt_vc_json\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String sessionId = response.split("\"sessionId\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/sessions/" + sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void testGetSessionStatusNotFound() throws Exception {
        mockMvc.perform(get("/sessions/non-existent-id"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testCancelSession() throws Exception {
        String response = mockMvc.perform(post("/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transportType\":\"websocket\",\"credentialType\":\"jwt_vc_json\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String sessionId = response.split("\"sessionId\":\"")[1].split("\"")[0];

        mockMvc.perform(delete("/sessions/" + sessionId))
            .andExpect(status().isNoContent());
    }

    @Test
    void testCancelSessionNotFound() throws Exception {
        mockMvc.perform(delete("/sessions/non-existent-id"))
            .andExpect(status().isNotFound());
    }
}
