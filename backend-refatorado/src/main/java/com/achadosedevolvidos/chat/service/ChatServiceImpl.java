package com.achadosedevolvidos.chat.service;

import com.achadosedevolvidos.chat.dto.ChatMessageRequest;
import com.achadosedevolvidos.chat.dto.ChatMessageResponse;
import com.achadosedevolvidos.chat.mapper.ChatMessageMapper;
import com.achadosedevolvidos.chat.model.Message;
import com.achadosedevolvidos.chat.repository.MessageRepository;
import com.achadosedevolvidos.match.model.Match;
import com.achadosedevolvidos.match.repository.MatchRepository;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * O protótipo original não validava, em nenhum lugar, se quem mandava a mensagem
 * tinha algo a ver com o match/conversa. Aqui isso é obrigatório: só o dono do
 * item perdido ou do item encontrado daquele match pode enviar mensagem nele.
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(UUID matchId, UUID senderId, ChatMessageRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new AppException("Match não encontrado", HttpStatus.NOT_FOUND));

        assertParticipant(match, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException("Usuário não encontrado", HttpStatus.UNAUTHORIZED));

        Message message = Message.builder()
                .match(match)
                .sender(sender)
                .content(request.content())
                .build();

        Message saved = messageRepository.save(message);
        return chatMessageMapper.toResponse(saved);
    }

    @Override
    public List<ChatMessageResponse> history(UUID matchId, UUID requesterId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new AppException("Match não encontrado", HttpStatus.NOT_FOUND));

        assertParticipant(match, requesterId);

        return messageRepository.findByMatchIdOrderByCreatedAtAsc(matchId)
                .stream()
                .map(chatMessageMapper::toResponse)
                .toList();
    }

    private void assertParticipant(Match match, UUID userId) {
        boolean isParticipant = match.getLostItem().getUser().getId().equals(userId)
                || match.getFoundItem().getUser().getId().equals(userId);

        if (!isParticipant) {
            throw new AppException("Usuário não participa desta conversa", HttpStatus.FORBIDDEN);
        }
    }
}
