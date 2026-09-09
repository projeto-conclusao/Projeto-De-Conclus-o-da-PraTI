package com.achadosedevolvidos.chat.mapper;

import com.achadosedevolvidos.chat.dto.ChatMessageResponse;
import com.achadosedevolvidos.chat.model.Message;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {

    public ChatMessageResponse toResponse(Message message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getMatch().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
