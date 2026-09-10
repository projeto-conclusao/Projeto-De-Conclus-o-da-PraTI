package com.achadosedevolvidos.chat.websocket;

import com.achadosedevolvidos.category.model.Category;
import com.achadosedevolvidos.category.repository.CategoryRepository;
import com.achadosedevolvidos.chat.dto.ChatMessageRequest;
import com.achadosedevolvidos.chat.dto.ChatMessageResponse;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.repository.ItemRepository;
import com.achadosedevolvidos.match.model.Match;
import com.achadosedevolvidos.match.repository.MatchRepository;
import com.achadosedevolvidos.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração real de WebSocket/STOMP (não MockMvc — precisa de um servidor de
 * verdade na porta aleatória do {@code @SpringBootTest}). Cobre exatamente a
 * regra de segurança que este refactor adicionou e que o protótipo original não
 * tinha nenhuma: {@link StompAuthChannelInterceptor} exigindo um Bearer JWT
 * válido no frame STOMP CONNECT (ver ARQUITETURA.md, seção 4).
 */
class ChatWebSocketIT extends IntegrationTestSupport {

    private static final UUID CATEGORIA_ROUPAS = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void sunnyDay_conectarComTokenValidoEnviarEReceberMensagemNoTopico() throws Exception {
        AuthenticatedTestUser remetente = registerAndAuthenticate("Remetente WS");
        AuthenticatedTestUser destinatario = registerAndAuthenticate("Destinatario WS");
        Match match = criarMatchEntre(remetente, destinatario);

        WebSocketStompClient stompClient = buildStompClient();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", remetente.authorizationHeader());

        StompSession session = stompClient
                .connectAsync(wsUrl(), new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        try {
            BlockingQueue<ChatMessageResponse> received = new LinkedBlockingQueue<>();
            session.subscribe("/topic/conversation/" + match.getId(), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return ChatMessageResponse.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.add((ChatMessageResponse) payload);
                }
            });

            // Dá tempo da assinatura ser processada pelo broker antes de mandar.
            Thread.sleep(300);

            session.send("/app/chat.sendMessage/" + match.getId(), new ChatMessageRequest("Encontrei seu casaco!"));

            ChatMessageResponse mensagem = received.poll(10, TimeUnit.SECONDS);

            assertThat(mensagem).isNotNull();
            assertThat(mensagem.content()).isEqualTo("Encontrei seu casaco!");
            assertThat(mensagem.senderId()).isEqualTo(remetente.user().getId());
            assertThat(mensagem.matchId()).isEqualTo(match.getId());
        } finally {
            session.disconnect();
        }
    }

    @Test
    void rainyDay_conexaoSemTokenDeveSerRecusada() {
        WebSocketStompClient stompClient = buildStompClient();

        CompletableFuture<StompSession> connectFuture = stompClient.connectAsync(
                wsUrl(), new WebSocketHttpHeaders(), new StompHeaders(), new StompSessionHandlerAdapter() {}
        );

        assertThatThrownBy(() -> connectFuture.get(5, TimeUnit.SECONDS))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    @Test
    void rainyDay_conexaoComTokenInvalidoDeveSerRecusada() {
        WebSocketStompClient stompClient = buildStompClient();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer isto-nao-eh-um-jwt-valido");

        CompletableFuture<StompSession> connectFuture = stompClient.connectAsync(
                wsUrl(), new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {}
        );

        assertThatThrownBy(() -> connectFuture.get(5, TimeUnit.SECONDS))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    private String wsUrl() {
        return "http://localhost:" + port + "/ws";
    }

    private WebSocketStompClient buildStompClient() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        // Reaproveita o ObjectMapper já configurado pelo Spring Boot (com
        // JavaTimeModule para LocalDateTime) em vez de um `new ObjectMapper()` cru
        // — sem isso, a desserialização do ChatMessageResponse.sentAt falhava
        // silenciosamente e o StompFrameHandler nunca era chamado.
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
        return stompClient;
    }

    private Match criarMatchEntre(AuthenticatedTestUser dono1, AuthenticatedTestUser dono2) {
        Category categoria = categoryRepository.findById(CATEGORIA_ROUPAS).orElseThrow();

        Item lostItem = itemRepository.save(Item.builder()
                .user(dono1.user()).category(categoria).type(Item.ItemType.PERDIDO)
                .title("Casaco perdido " + UUID.randomUUID())
                .description("Casaco perdido para teste")
                .shortDescription("Casaco perdido")
                .eventDate(LocalDateTime.now().minusHours(1))
                .build());

        Item foundItem = itemRepository.save(Item.builder()
                .user(dono2.user()).category(categoria).type(Item.ItemType.ENCONTRADO)
                .title("Casaco encontrado " + UUID.randomUUID())
                .description("Casaco encontrado para teste")
                .shortDescription("Casaco encontrado")
                .eventDate(LocalDateTime.now())
                .build());

        return matchRepository.save(Match.builder().lostItem(lostItem).foundItem(foundItem).score(90.0).build());
    }
}
