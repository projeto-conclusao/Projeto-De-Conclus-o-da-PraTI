package com.achadosedevolvidos.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Propositalmente sem senderId: no protótipo original, o ChatController confiava
 * em um campo enviado pelo próprio cliente para identificar o remetente — o que
 * permitiria qualquer pessoa enviar mensagem se passando por outro usuário. Agora
 * o remetente vem do Principal autenticado na conexão STOMP.
 */
public record ChatMessageRequest(
        @NotBlank(message = "Mensagem não pode ser vazia")
        String content
) {}
