package com.achadosedevolvidos.item.controller;

import com.achadosedevolvidos.item.dto.CreateItemRequest;
import com.achadosedevolvidos.item.dto.ItemResponse;
import com.achadosedevolvidos.item.model.Item;
import com.achadosedevolvidos.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integração de ponta a ponta de {@code ItemController}: criação, busca e
 * detalhe, incluindo a exigência de autenticação via {@code @AuthenticationPrincipal}
 * que este refactor corrigiu (ver ARQUITETURA.md, seção 4).
 */
class ItemControllerIT extends IntegrationTestSupport {

    private static final UUID CATEGORIA_ELETRONICOS = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void sunnyDay_deveCriarBuscarEConsultarItemAutenticado() throws Exception {
        AuthenticatedTestUser user = registerAndAuthenticate("Dono do Item");
        String marcador = "Marcador-" + UUID.randomUUID();

        CreateItemRequest request = new CreateItemRequest(
                Item.ItemType.PERDIDO, CATEGORIA_ELETRONICOS, marcador + " carteira preta",
                "Perdida perto da entrada principal", "Bloco A", -23.5505, -46.6333,
                LocalDateTime.now().minusHours(2), List.of("http://exemplo.com/foto1.png")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", user.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(marcador + " carteira preta"))
                .andExpect(jsonPath("$.status").value("ANALISANDO"))
                .andReturn();

        ItemResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), ItemResponse.class);

        mockMvc.perform(get("/api/v1/items/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()));

        mockMvc.perform(get("/api/v1/items/search")
                        .param("type", "PERDIDO")
                        .param("query", marcador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + created.id() + "')]").exists());

        // Endpoint autenticado (não é o dono de nada além do próprio item, mas
        // qualquer usuário logado pode consultar matches de um item público).
        mockMvc.perform(get("/api/v1/items/" + created.id() + "/matches")
                        .header("Authorization", user.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void rainyDay_deveRecusarCriacaoSemAutenticacao() throws Exception {
        CreateItemRequest request = new CreateItemRequest(
                Item.ItemType.PERDIDO, CATEGORIA_ELETRONICOS, "Item sem dono",
                null, null, -23.55, -46.63, LocalDateTime.now(), null
        );

        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Sem header Authorization nenhum (nem "Bearer" inválido), o CsrfFilter
                // intercepta antes mesmo da autenticação: POST fora de /api/v1/auth/**
                // e sem "Authorization: Bearer ..." não está na lista de
                // ignoringRequestMatchers do SecurityConfig, e sem cookie/; header
                // XSRF-TOKEN a requisição é barrada com 403 (ver rainyDay abaixo com
                // Bearer inválido, que já ignora CSRF e cai no redirect do oauth2Login).
                .andExpect(status().isForbidden());
    }

    @Test
    void rainyDay_deveRecusarConsultaDeMatchesSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/items/" + UUID.randomUUID() + "/matches"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void rainyDay_deveRecusarCriacaoComCategoriaInexistente() throws Exception {
        AuthenticatedTestUser user = registerAndAuthenticate("Categoria Invalida");

        CreateItemRequest request = new CreateItemRequest(
                Item.ItemType.PERDIDO, UUID.randomUUID(), "Item com categoria fantasma",
                null, null, -23.55, -46.63, LocalDateTime.now(), null
        );

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", user.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rainyDay_deveRecusarCriacaoComPayloadInvalido() throws Exception {
        AuthenticatedTestUser user = registerAndAuthenticate("Payload Invalido");

        // type e categoryId ausentes, título em branco, data no futuro.
        String payloadInvalido = """
                {
                  "title": "",
                  "latitude": -23.55,
                  "longitude": -46.63,
                  "eventDate": "2099-01-01T00:00:00"
                }
                """;

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", user.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rainyDay_deveRetornar404ParaItemInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/items/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rainyDay_deveIgnorarTokenInvalidoERecusarComoAnonimo() throws Exception {
        CreateItemRequest request = new CreateItemRequest(
                Item.ItemType.PERDIDO, CATEGORIA_ELETRONICOS, "Item com token quebrado",
                null, null, -23.55, -46.63, LocalDateTime.now(), null
        );

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer isto-nao-eh-um-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void sunnyDay_buscaSemFiltrosNaoQuebra() throws Exception {
        mockMvc.perform(get("/api/v1/items/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
