package com.achadosedevolvidos.chat.controller;

import com.achadosedevolvidos.chat.dto.ChatMessageRequest;
import com.achadosedevolvidos.chat.dto.ChatMessageResponse;
import com.achadosedevolvidos.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * O "conversationId" do protótipo original é aqui o id do Match (ver
 * {@code chat.model.Message}). O cliente conecta em /ws, envia para
 * /app/chat.sendMessage/{matchId} e assina /topic/conversation/{matchId} para
 * receber as mensagens em tempo real.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage/{matchId}")
    public void sendMessage(
            @DestinationVariable UUID matchId,
            @Payload ChatMessageRequest request,
            Principal principal
    ) {
        UUID senderId = UUID.fromString(principal.getName());
        ChatMessageResponse saved = chatService.sendMessage(matchId, senderId, request);
        messagingTemplate.convertAndSend("/topic/conversation/" + matchId, saved);
    }
}
