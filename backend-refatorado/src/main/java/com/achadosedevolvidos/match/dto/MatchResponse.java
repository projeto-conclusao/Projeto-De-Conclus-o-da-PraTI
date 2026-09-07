package com.achadosedevolvidos.match.dto;

import com.achadosedevolvidos.match.model.Match;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID lostItemId,
        String lostItemTitle,
        UUID foundItemId,
        String foundItemTitle,
        Double score,
        Match.MatchStatus status,
        LocalDateTime createdAt
) {}
