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
class PresentationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSubmitPresentation() throws Exception {
        String presentation = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkaWQ6d2Vidmg6aG9sZGVyIiwidnAiOnsidmVyaWZpYWJsZUNyZWRlbnRpYWwiOltdfX0.signature";

        mockMvc.perform(post("/presentations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"presentation\":\"" + presentation + "\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void testSubmitPresentationEmpty() throws Exception {
        mockMvc.perform(post("/presentations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"presentation\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
}
