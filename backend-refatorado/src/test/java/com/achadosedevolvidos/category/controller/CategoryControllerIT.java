package com.achadosedevolvidos.category.controller;

import com.achadosedevolvidos.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint público e somente-leitura: as categorias vêm da seed do Flyway (V7),
 * então o "sunny day" é a lista batendo com o que a migration insere.
 */
class CategoryControllerIT extends IntegrationTestSupport {

    @Test
    void sunnyDay_deveListarCategoriasSeedadasPeloFlyway_semExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'Eletrônicos')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Documentos')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Chaves')]").exists());
    }

    @Test
    void rainyDay_caminhoInexistenteSobPrefixoDeCategoriasDeveRetornar404() throws Exception {
        // Rota GET liberada ao security por /api/v1/categories/** (permitAll),
        // mas sem nenhum @GetMapping de Controller que a atenda.
        mockMvc.perform(get("/api/v1/categories/isto-nao-existe"))
                .andExpect(status().isNotFound());
    }
}
