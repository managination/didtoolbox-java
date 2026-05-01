package com.managination.numa.didserver.controller;

import com.managination.numa.didserver.dto.*;
import com.managination.numa.didserver.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/claim-reward")
@Tag(name = "Rewards", description = "Reward claiming and NFT issuance endpoints")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @PostMapping
    @Operation(summary = "Claim a reward by submitting a verifiable presentation", operationId = "claimReward")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward claimed successfully, NFT issued",
            content = @Content(schema = @Schema(implementation = ClaimRewardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid claim request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Presentation does not satisfy DCQL requirements",
            content = @Content(schema = @Schema(implementation = ClaimRewardResponse.class))),
        @ApiResponse(responseCode = "404", description = "Reward not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> claimReward(@RequestBody ClaimRewardRequest request) {
        if (request.rewardId() == null || request.rewardId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", "rewardId is required"));
        }

        if (request.presentation() == null || request.presentation().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_request", "presentation is required"));
        }

        RewardService.RewardDefinition reward;
        try {
            reward = rewardService.getReward(request.rewardId());
        } catch (RewardService.RewardNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", e.getMessage()));
        }

        boolean valid = rewardService.validateDCQL(request.presentation(), reward.dcqlQuery());

        if (valid) {
            if (reward.credentialsToRevoke() != null && !reward.credentialsToRevoke().isEmpty()) {
                rewardService.revokeCredentials(reward.credentialsToRevoke());
            }

            NftTransaction nftTx = new NftTransaction(
                UUID.randomUUID().toString(),
                "1",
                reward.nftContractAddress(),
                "0xabcdef1234567890abcdef1234567890abcdef12",
                1
            );

            return ResponseEntity.ok(new ClaimRewardResponse(
                true,
                "Reward claimed successfully",
                nftTx,
                null,
                reward.credentialsToRevoke()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ClaimRewardResponse(
                false,
                "Presentation does not satisfy DCQL requirements",
                null,
                List.of("DCQL requirements not met"),
                List.of()
            ));
        }
    }
}
