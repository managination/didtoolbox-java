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
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testClaimReward() throws Exception {
        String body = """
            {
                "rewardId": "sample-reward-001",
                "presentation": "{\\"verifiableCredential\\":[]}"
            }
            """;

        mockMvc.perform(post("/claim-reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testClaimRewardNotFound() throws Exception {
        String body = """
            {
                "rewardId": "non-existent-reward",
                "presentation": "vp-with-credentials"
            }
            """;

        mockMvc.perform(post("/claim-reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void testClaimRewardMissingFields() throws Exception {
        String body = """
            {
                "presentation": "vp-with-credentials"
            }
            """;

        mockMvc.perform(post("/claim-reward")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
