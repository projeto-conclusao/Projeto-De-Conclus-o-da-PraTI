package com.achadosedevolvidos.chat.controller;

import com.achadosedevolvidos.category.model.Category;
import com.achadosedevolvidos.category.repository.CategoryRepository;
import com.achadosedevolvidos.chat.dto.ChatMessageRequest;
import com.achadosedevolvidos.chat.service.ChatService;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.item.repository.ItemRepository;
import com.achadosedevolvidos.match.model.Match;
import com.achadosedevolvidos.match.repository.MatchRepository;
import com.achadosedevolvidos.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integração do histórico REST do chat. O envio de mensagens em si acontece só
 * via WebSocket/STOMP (ver {@link com.achadosedevolvidos.chat.websocket
 * .ChatWebSocketIT}); aqui a mensagem é semeada diretamente pelo {@link ChatService}
 * real (mesma validação de participante, só que sem precisar de um cliente STOMP)
 * para focar no que é específico deste endpoint: quem pode ler o histórico.
 */
class ChatHistoryControllerIT extends IntegrationTestSupport {

    private static final UUID CATEGORIA_DOCUMENTOS = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ChatService chatService;

    @Test
    void sunnyDay_participanteDeveLerHistoricoDaConversa() throws Exception {
        AuthenticatedTestUser donoDoPerdido = registerAndAuthenticate("Participante Um");
        AuthenticatedTestUser donoDoEncontrado = registerAndAuthenticate("Participante Dois");
        Match match = criarMatchEntre(donoDoPerdido, donoDoEncontrado);

        chatService.sendMessage(match.getId(), donoDoPerdido.user().getId(), new ChatMessageRequest("Olá, é minha carteira!"));
        chatService.sendMessage(match.getId(), donoDoEncontrado.user().getId(), new ChatMessageRequest("Pode descrever ela?"));

        mockMvc.perform(get("/api/v1/matches/" + match.getId() + "/messages")
                        .header("Authorization", donoDoPerdido.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Olá, é minha carteira!"))
                .andExpect(jsonPath("$[1].content").value("Pode descrever ela?"));
    }

    @Test
    void rainyDay_naoParticipanteNaoPodeLerHistorico() throws Exception {
        AuthenticatedTestUser donoDoPerdido = registerAndAuthenticate("Dono Legitimo");
        AuthenticatedTestUser donoDoEncontrado = registerAndAuthenticate("Outro Dono Legitimo");
        AuthenticatedTestUser estranho = registerAndAuthenticate("Estranho");
        Match match = criarMatchEntre(donoDoPerdido, donoDoEncontrado);

        mockMvc.perform(get("/api/v1/matches/" + match.getId() + "/messages")
                        .header("Authorization", estranho.authorizationHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rainyDay_matchInexistenteDeveRetornar404() throws Exception {
        AuthenticatedTestUser usuario = registerAndAuthenticate("Match Fantasma");

        mockMvc.perform(get("/api/v1/matches/" + UUID.randomUUID() + "/messages")
                        .header("Authorization", usuario.authorizationHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rainyDay_semAutenticacaoDeveSerRedirecionado() throws Exception {
        mockMvc.perform(get("/api/v1/matches/" + UUID.randomUUID() + "/messages"))
                .andExpect(status().is3xxRedirection());
    }

    private Match criarMatchEntre(AuthenticatedTestUser dono1, AuthenticatedTestUser dono2) {
        Category categoria = categoryRepository.findById(CATEGORIA_DOCUMENTOS).orElseThrow();

        Item lostItem = itemRepository.save(Item.builder()
                .user(dono1.user()).category(categoria).type(Item.ItemType.PERDIDO)
                .title("Documento perdido " + UUID.randomUUID())
                .description("Documento perdido para teste")
                .shortDescription("Documento perdido")
                .eventDate(LocalDateTime.now().minusHours(1))
                .build());

        Item foundItem = itemRepository.save(Item.builder()
                .user(dono2.user()).category(categoria).type(Item.ItemType.ENCONTRADO)
                .title("Documento encontrado " + UUID.randomUUID())
                .description("Documento encontrado para teste")
                .shortDescription("Documento encontrado")
                .eventDate(LocalDateTime.now())
                .build());

        return matchRepository.save(Match.builder().lostItem(lostItem).foundItem(foundItem).score(90.0).build());
    }
}
