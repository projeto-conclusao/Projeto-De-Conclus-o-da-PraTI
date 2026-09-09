package com.achadosedevolvidos.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID matchId,
        UUID senderId,
        String senderName,
        String content,
        LocalDateTime sentAt
) {}
