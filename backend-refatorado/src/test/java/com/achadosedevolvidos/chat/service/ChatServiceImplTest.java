package com.achadosedevolvidos.chat.service;

import com.achadosedevolvidos.chat.dto.ChatMessageRequest;
import com.achadosedevolvidos.chat.dto.ChatMessageResponse;
import com.achadosedevolvidos.chat.mapper.ChatMessageMapper;
import com.achadosedevolvidos.chat.model.Message;
import com.achadosedevolvidos.chat.repository.MessageRepository;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.match.model.Match;
import com.achadosedevolvidos.match.repository.MatchRepository;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre a regra que o protótipo original não tinha: só os dois donos de item do
 * match (perdido/encontrado) podem mandar mensagem ou ler o histórico da conversa.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    private ChatServiceImpl chatService;

    private User donoDoPerdido;
    private User donoDoEncontrado;
    private User estranho;
    private Match match;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(matchRepository, userRepository, messageRepository, new ChatMessageMapper());

        donoDoPerdido = User.builder().id(UUID.randomUUID()).name("Ana").email("ana@teste.com")
                .role(User.Role.USER).provider(User.AuthProvider.LOCAL).build();
        donoDoEncontrado = User.builder().id(UUID.randomUUID()).name("Bruno").email("bruno@teste.com")
                .role(User.Role.USER).provider(User.AuthProvider.LOCAL).build();
        estranho = User.builder().id(UUID.randomUUID()).name("Carlos").email("carlos@teste.com")
                .role(User.Role.USER).provider(User.AuthProvider.LOCAL).build();

        Item lostItem = Item.builder().id(UUID.randomUUID()).user(donoDoPerdido).title("Carteira").build();
        Item foundItem = Item.builder().id(UUID.randomUUID()).user(donoDoEncontrado).title("Carteira achada").build();
        match = Match.builder().id(UUID.randomUUID()).lostItem(lostItem).foundItem(foundItem).score(80.0).build();
    }

    // ---------- sendMessage() ----------

    @Test
    void sunnyDay_deveEnviarMensagemQuandoRemetenteEhParticipanteDoMatch() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(donoDoPerdido.getId())).thenReturn(Optional.of(donoDoPerdido));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            message.setCreatedAt(LocalDateTime.now());
            return message;
        });

        ChatMessageResponse response = chatService.sendMessage(
                match.getId(), donoDoPerdido.getId(), new ChatMessageRequest("Oi, acho que achei sua carteira!")
        );

        assertThat(response.senderId()).isEqualTo(donoDoPerdido.getId());
        assertThat(response.content()).isEqualTo("Oi, acho que achei sua carteira!");
        assertThat(response.matchId()).isEqualTo(match.getId());
    }

    @Test
    void rainyDay_deveNegarEnvioQuandoRemetenteNaoParticipaDoMatch() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> chatService.sendMessage(
                match.getId(), estranho.getId(), new ChatMessageRequest("Deixa eu entrar nessa conversa")
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void rainyDay_deveLancarNotFoundQuandoMatchNaoExisteAoEnviar() {
        UUID idInexistente = UUID.randomUUID();
        when(matchRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(
                idInexistente, donoDoPerdido.getId(), new ChatMessageRequest("Oi")
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    // ---------- history() ----------

    @Test
    void sunnyDay_deveRetornarHistoricoParaParticipanteDoMatch() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        Message message = Message.builder().id(UUID.randomUUID()).match(match).sender(donoDoEncontrado)
                .content("Achei sim!").createdAt(LocalDateTime.now()).build();
        when(messageRepository.findByMatchIdOrderByCreatedAtAsc(match.getId())).thenReturn(List.of(message));

        List<ChatMessageResponse> historico = chatService.history(match.getId(), donoDoEncontrado.getId());

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).content()).isEqualTo("Achei sim!");
    }

    @Test
    void rainyDay_deveNegarHistoricoQuandoUsuarioNaoParticipaDoMatch() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> chatService.history(match.getId(), estranho.getId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(messageRepository, never()).findByMatchIdOrderByCreatedAtAsc(any());
    }

    @Test
    void rainyDay_deveLancarNotFoundQuandoMatchNaoExisteAoLerHistorico() {
        UUID idInexistente = UUID.randomUUID();
        when(matchRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.history(idInexistente, donoDoPerdido.getId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }
}
