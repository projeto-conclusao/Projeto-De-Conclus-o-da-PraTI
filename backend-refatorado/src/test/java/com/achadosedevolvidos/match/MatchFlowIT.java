package com.achadosedevolvidos.match;

import com.achadosedevolvidos.item.dto.CreateItemRequest;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.match.dto.MatchResponse;
import com.achadosedevolvidos.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o fluxo completo item -> evento -> match de ponta a ponta, incluindo o
 * processamento assíncrono (listener {@code @Async} + {@code AFTER_COMMIT}) —
 * item explicitamente listado como pendente em ARQUITETURA.md antes deste
 * refactor de testes.
 *
 * <p>Usa a categoria "Chaves" (fixa, semeada pelo Flyway) com exclusividade nesta
 * classe para não competir com itens criados por outras classes de teste que
 * usam "Eletrônicos" — como o motor de match só considera candidatos da mesma
 * categoria, isso isola os testes sem precisar de transação/rollback (que
 * quebraria o listener AFTER_COMMIT).
 */
class MatchFlowIT extends IntegrationTestSupport {

    private static final UUID CATEGORIA_CHAVES = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void sunnyDay_itemPerdidoEEncontradoCompativeisDevemGerarMatchAssincronamente() throws Exception {
        AuthenticatedTestUser donoDoPerdido = registerAndAuthenticate("Dono Perdido");
        AuthenticatedTestUser donoDoEncontrado = registerAndAuthenticate("Dono Encontrado");

        String marcador = "Chave-" + UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now().minusHours(1);

        ItemResponse perdido = criarItem(donoDoPerdido, new CreateItemRequest(
                Item.ItemType.PERDIDO, CATEGORIA_CHAVES, marcador + " chave do apartamento",
                "Perdida no bolso da calça", "Chave do apartamento", "Rua A, 100", -23.5505, -46.6333, agora, null
        ));

        criarItem(donoDoEncontrado, new CreateItemRequest(
                Item.ItemType.ENCONTRADO, CATEGORIA_CHAVES, marcador + " molho de chaves achado",
                "Achado próximo à esquina", "Molho de chaves achado", "Rua A, 120", -23.5507, -46.6335, agora.plusHours(1), null
        ));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<MatchResponse> matches = buscarMatches(donoDoPerdido, perdido.id());
            assertThat(matches).isNotEmpty();
            assertThat(matches.get(0).score()).isGreaterThanOrEqualTo(40.0);
            assertThat(matches.get(0).lostItemId()).isEqualTo(perdido.id());
        });
    }

    @Test
    void rainyDay_doisItensDoMesmoTipoNuncaDevemGerarMatch() throws Exception {
        AuthenticatedTestUser usuario = registerAndAuthenticate("Dois Perdidos");
        String marcador = "Chave-" + UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now().minusHours(1);

        ItemResponse primeiroPerdido = criarItem(usuario, new CreateItemRequest(
                Item.ItemType.PERDIDO, CATEGORIA_CHAVES, marcador + " chave 1",
                "Perdida em algum lugar", "Chave 1 perdida", null, -23.5505, -46.6333, agora, null
        ));
        criarItem(usuario, new CreateItemRequest(
                Item.ItemType.PERDIDO, CATEGORIA_CHAVES, marcador + " chave 2",
                "Perdida em algum lugar", "Chave 2 perdida", null, -23.5505, -46.6333, agora, null
        ));

        // Não há evento "match não vai acontecer" para esperar — damos um tempo
        // razoável de processamento assíncrono e então confirmamos que continua
        // vazio (o cálculo de score para mesmo tipo é 0 e retorna quase
        // instantaneamente, então essa janela é folgada o bastante).
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<MatchResponse> matches = buscarMatches(usuario, primeiroPerdido.id());
            assertThat(matches).isEmpty();
        });
    }

    private ItemResponse criarItem(AuthenticatedTestUser autor, CreateItemRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", autor.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ItemResponse.class);
    }

    private List<MatchResponse> buscarMatches(AuthenticatedTestUser usuario, UUID itemId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/items/" + itemId + "/matches")
                        .header("Authorization", usuario.authorizationHeader()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, MatchResponse.class)
        );
    }
}
