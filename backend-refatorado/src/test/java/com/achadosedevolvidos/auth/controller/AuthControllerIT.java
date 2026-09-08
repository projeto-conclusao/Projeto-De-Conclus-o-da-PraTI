package com.achadosedevolvidos.auth.controller;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.LoginRequest;
import com.achadosedevolvidos.auth.dto.RefreshRequest;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integração de ponta a ponta do mecanismo Bearer JWT: HTTP real -> Controller ->
 * Service -> Postgres real (Testcontainers) -> resposta HTTP real. Um dia de sol
 * e os principais dias de chuva de cada endpoint.
 */
class AuthControllerIT extends IntegrationTestSupport {

    @Test
    void sunnyDay_deveRegistrarLogarERenovarToken() throws Exception {
        String email = "fluxo-completo-" + UUID.randomUUID() + "@teste.com";
        RegisterRequest registerRequest = new RegisterRequest("Ana Silva", email, "senha12345");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        AuthResponse registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(), AuthResponse.class
        );

        LoginRequest loginRequest = new LoginRequest(email, "senha12345");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        RefreshRequest refreshRequest = new RefreshRequest(registerResponse.refreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void rainyDay_deveRecusarRegistroComEmailJaCadastrado() throws Exception {
        String email = "duplicado-" + UUID.randomUUID() + "@teste.com";
        RegisterRequest request = new RegisterRequest("Ana Silva", email, "senha12345");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void rainyDay_deveRecusarRegistroComPayloadInvalido() throws Exception {
        RegisterRequest request = new RegisterRequest("", "nao-e-um-email", "123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rainyDay_deveRecusarLoginComSenhaErrada() throws Exception {
        String email = "senha-errada-" + UUID.randomUUID() + "@teste.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Ana Silva", email, "senha12345"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "senha-totalmente-errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rainyDay_deveRecusarLoginDeEmailInexistente() throws Exception {
        LoginRequest request = new LoginRequest("nao-existe-" + UUID.randomUUID() + "@teste.com", "qualquercoisa");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rainyDay_deveRecusarRefreshComTokenMalformado() throws Exception {
        RefreshRequest request = new RefreshRequest("isto-nao-eh-um-jwt-valido");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rainyDay_respostaDeErroNuncaExpoeStackTrace() throws Exception {
        RegisterRequest request = new RegisterRequest("", "nao-e-um-email", "123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Exception")
                .doesNotContain("com.achadosedevolvidos");
    }
}
