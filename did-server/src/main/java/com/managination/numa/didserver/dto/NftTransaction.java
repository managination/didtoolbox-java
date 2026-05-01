package com.managination.numa.didserver.dto;

public record NftTransaction(
    String txHash,
    String tokenId,
    String contractAddress,
    String recipientAddress,
    Integer chainId
) {}
