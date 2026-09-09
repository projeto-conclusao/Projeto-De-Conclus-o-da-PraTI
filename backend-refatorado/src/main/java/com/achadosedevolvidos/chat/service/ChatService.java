package com.achadosedevolvidos.chat.service;

import com.achadosedevolvidos.chat.dto.ChatMessageRequest;
import com.achadosedevolvidos.chat.dto.ChatMessageResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatMessageResponse sendMessage(UUID matchId, UUID senderId, ChatMessageRequest request);

    List<ChatMessageResponse> history(UUID matchId, UUID requesterId);
}
