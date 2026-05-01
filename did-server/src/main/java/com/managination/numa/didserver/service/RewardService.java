package com.managination.numa.didserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RewardService {

    public record RewardDefinition(
        String id,
        String name,
        String dcqlQuery,
        List<String> credentialsToRevoke,
        String nftContractAddress,
        boolean active
    ) {}

    private final ConcurrentHashMap<String, RewardDefinition> rewardStore = new ConcurrentHashMap<>();
    private final Set<String> revokedCredentials = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RewardDefinition getReward(String rewardId) {
        RewardDefinition reward = rewardStore.get(rewardId);
        if (reward == null) {
            throw new RewardNotFoundException("Reward not found: " + rewardId);
        }
        return reward;
    }

    public void registerReward(RewardDefinition reward) {
        rewardStore.put(reward.id(), reward);
    }

    public boolean validateDCQL(String presentation, String dcqlQuery) {
        try {
            JsonNode vpNode = objectMapper.readTree(presentation);
            if (vpNode.has("verifiableCredential") || vpNode.has("vp")) {
                return true;
            }
            return vpNode.isTextual() && !vpNode.asText().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeCredentials(List<String> credentialIds) {
        if (credentialIds != null) {
            revokedCredentials.addAll(credentialIds);
        }
    }

    public boolean isCredentialRevoked(String credentialId) {
        return revokedCredentials.contains(credentialId);
    }

    @PostConstruct
    public void init() {
        RewardDefinition sampleReward = new RewardDefinition(
            "sample-reward-001",
            "Early Adopter Badge",
            "{\"credentials\":[{\"format\":\"jwt_vc_json\",\"meta\":{\"vct\":{\"values\":[\"VerifiableCredential\"]}}}]}",
            List.of(),
            "0x1234567890abcdef1234567890abcdef12345678",
            true
        );
        rewardStore.put(sampleReward.id(), sampleReward);
    }

    public static class RewardNotFoundException extends RuntimeException {
        public RewardNotFoundException(String message) {
            super(message);
        }
    }
}
