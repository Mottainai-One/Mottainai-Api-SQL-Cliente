package com.institutojf.mottainai.client.dto.response;

public record CustomerLoyaltyRewardResponse(
        Integer id,
        String name,
        String description,
        Integer pointsCost,
        boolean eligible,
        int pointsShortfall
) {
}
