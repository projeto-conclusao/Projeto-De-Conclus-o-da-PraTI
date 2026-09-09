package com.achadosedevolvidos.auth.service;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.LoginRequest;
import com.achadosedevolvidos.auth.dto.RefreshRequest;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários puros (Mockito): nenhuma dependência sobe Spring nem banco.
 * Cobre os três fluxos do mecanismo Bearer JWT — register/login/refresh — com
 * um caminho "sunny day" e os "rainy days" correspondentes de cada um.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long ACCESS_EXPIRATION_MS = 900_000L;
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
        ReflectionTestUtils.setField(authService, "accessExpirationMs", ACCESS_EXPIRATION_MS);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", REFRESH_EXPIRATION_MS);
    }

    private void stubGeracaoDeTokens() {
        when(jwtService.generateToken(any(), anyMap(), eq(ACCESS_EXPIRATION_MS))).thenReturn("access-token");
        when(jwtService.generateToken(any(), anyMap(), eq(REFRESH_EXPIRATION_MS))).thenReturn("refresh-token");
    }

    // ---------- register() ----------

    @Test
    void sunnyDay_deveRegistrarUsuarioEGerarTokens() {
        stubGeracaoDeTokens();
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@teste.com", "senha12345");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hash-da-senha");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(ACCESS_EXPIRATION_MS / 1000);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rainyDay_deveLancarConflitoQuandoEmailJaCadastrado() {
        RegisterRequest request = new RegisterRequest("Ana Silva", "ana@teste.com", "senha12345");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any());
    }

    // ---------- login() ----------

    @Test
    void sunnyDay_deveLogarUsuarioLocalComCredenciaisValidas() {
        stubGeracaoDeTokens();
        User user = localUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest(user.getEmail(), "senha12345"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void rainyDay_deveLancarBadCredentialsQuandoUsuarioNaoExiste() {
        when(userRepository.findByEmail("fantasma@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("fantasma@teste.com", "qualquer")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rainyDay_deveLancarConflitoQuandoContaEhSomenteGoogle() {
        User googleUser = localUser();
        googleUser.setProvider(User.AuthProvider.GOOGLE);
        googleUser.setPassword(null);
        when(userRepository.findByEmail(googleUser.getEmail())).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.login(new LoginRequest(googleUser.getEmail(), "qualquer")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void rainyDay_devePropagarExcecaoQuandoSenhaNaoConfere() {
        User user = localUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "senha-errada")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ---------- refreshToken() ----------

    @Test
    void sunnyDay_deveRenovarTokensComRefreshTokenValido() {
        stubGeracaoDeTokens();
        User user = localUser();
        when(jwtService.extractUsername("refresh-valido")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-valido", user)).thenReturn(true);

        AuthResponse response = authService.refreshToken(new RefreshRequest("refresh-valido"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void rainyDay_deveLancarUnauthorizedQuandoTokenMalformado() {
        when(jwtService.extractUsername("token-invalido")).thenThrow(new MalformedJwtException("token quebrado"));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshRequest("token-invalido")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rainyDay_deveLancarUnauthorizedQuandoUsuarioDoTokenNaoExisteMais() {
        when(jwtService.extractUsername("token-de-usuario-deletado")).thenReturn("deletado@teste.com");
        when(userRepository.findByEmail("deletado@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(new RefreshRequest("token-de-usuario-deletado")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rainyDay_deveLancarUnauthorizedQuandoTokenNaoEhMaisValido() {
        User user = localUser();
        when(jwtService.extractUsername("refresh-expirado")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-expirado", user)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(new RefreshRequest("refresh-expirado")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    private User localUser() {
        return User.builder()
                .name("Ana Silva")
                .email("ana@teste.com")
                .password("hash-da-senha")
                .role(User.Role.USER)
                .provider(User.AuthProvider.LOCAL)
                .build();
    }
}
