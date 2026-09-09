package com.achadosedevolvidos.chat.controller;

import com.achadosedevolvidos.chat.dto.ChatMessageResponse;
import com.achadosedevolvidos.chat.service.ChatService;
import com.achadosedevolvidos.user.model.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Complementa o ChatController (STOMP): o WebSocket entrega mensagens em tempo
 * real, mas o front-end precisa de um jeito comum de carregar o histórico ao
 * abrir a conversa — daí este endpoint REST.
 */
@RestController
@RequestMapping("/api/v1/matches/{matchId}/messages")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> history(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID matchId
    ) {
        return ResponseEntity.ok(chatService.history(matchId, currentUser.getId()));
    }
}
