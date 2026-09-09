package com.achadosedevolvidos.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        String secret = Base64.getEncoder()
                .encodeToString("chave-de-teste-com-pelo-menos-256-bits-para-hs256!!".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);

        userDetails = User.withUsername("usuario@teste.com")
                .password("x")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void deveGerarTokenComOSubjectCorreto() {
        String token = jwtService.generateToken(userDetails, Map.of("role", "USER"), 60_000);

        assertThat(jwtService.extractUsername(token)).isEqualTo("usuario@teste.com");
    }

    @Test
    void deveValidarTokenRecemGerado() {
        String token = jwtService.generateToken(userDetails, Map.of(), 60_000);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void deveInvalidarTokenExpirado() throws InterruptedException {
        String token = jwtService.generateToken(userDetails, Map.of(), 1);
        Thread.sleep(10);

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void deveInvalidarTokenDeOutroUsuario() {
        String token = jwtService.generateToken(userDetails, Map.of(), 60_000);
        UserDetails outroUsuario = User.withUsername("outro@teste.com")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        assertThat(jwtService.isTokenValid(token, outroUsuario)).isFalse();
    }
}
