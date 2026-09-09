package com.achadosedevolvidos.auth.service;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.ForgotPasswordRequest;
import com.achadosedevolvidos.auth.dto.LoginRequest;
import com.achadosedevolvidos.auth.dto.LogoutRequest;
import com.achadosedevolvidos.auth.dto.MessageResponse;
import com.achadosedevolvidos.auth.dto.RefreshRequest;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.auth.dto.ResetPasswordRequest;
import com.achadosedevolvidos.auth.model.PasswordResetToken;
import com.achadosedevolvidos.auth.model.RefreshToken;
import com.achadosedevolvidos.auth.repository.PasswordResetTokenRepository;
import com.achadosedevolvidos.auth.repository.RefreshTokenRepository;
import com.achadosedevolvidos.auth.util.TokenHasher;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários puros (Mockito): nenhuma dependência sobe Spring nem banco.
 * Cobre os fluxos do mecanismo Bearer JWT — register/login/refresh/logout — e o
 * fluxo de esqueci-minha-senha, com um caminho "sunny day" e os "rainy days"
 * correspondentes de cada um.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long ACCESS_EXPIRATION_MS = 900_000L;
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;
    private static final long PASSWORD_RESET_EXPIRATION_MINUTES = 30L;
    private static final String PASSWORD_RESET_REDIRECT_URI = "http://localhost:5173/reset-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, authenticationManager,
                refreshTokenRepository, passwordResetTokenRepository, emailService
        );
        ReflectionTestUtils.setField(authService, "accessExpirationMs", ACCESS_EXPIRATION_MS);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", REFRESH_EXPIRATION_MS);
        ReflectionTestUtils.setField(authService, "passwordResetExpirationMinutes", PASSWORD_RESET_EXPIRATION_MINUTES);
        ReflectionTestUtils.setField(authService, "passwordResetRedirectUri", PASSWORD_RESET_REDIRECT_URI);
    }

    private void stubGeracaoDeTokens() {
        when(jwtService.generateToken(any(), anyMap(), eq(ACCESS_EXPIRATION_MS))).thenReturn("access-token");
        when(jwtService.generateToken(any(), anyMap(), eq(REFRESH_EXPIRATION_MS))).thenReturn("refresh-token");
    }

    private RefreshToken refreshTokenEntity(User user, LocalDateTime expiresAt, LocalDateTime revokedAt) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash("hash-qualquer")
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .build();
    }

    private PasswordResetToken passwordResetTokenEntity(User user, LocalDateTime expiresAt, LocalDateTime usedAt) {
        return PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash-qualquer")
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();
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
        verify(refreshTokenRepository).save(any(RefreshToken.class));
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
        verify(refreshTokenRepository).save(any(RefreshToken.class));
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
        RefreshToken storedToken = refreshTokenEntity(user, LocalDateTime.now().plusDays(1), null);
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash("refresh-valido")))
                .thenReturn(Optional.of(storedToken));

        AuthResponse response = authService.refreshToken(new RefreshRequest("refresh-valido"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(storedToken.getRevokedAt()).isNotNull();
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

    @Test
    void rainyDay_refreshDeveRecusarTokenRevogadoNoBanco() {
        User user = localUser();
        when(jwtService.extractUsername("refresh-revogado")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-revogado", user)).thenReturn(true);
        RefreshToken revokedToken = refreshTokenEntity(user, LocalDateTime.now().plusDays(1), LocalDateTime.now());
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash("refresh-revogado")))
                .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshRequest("refresh-revogado")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    // ---------- logout() ----------

    @Test
    void sunnyDay_deveRevogarRefreshTokenAoFazerLogout() {
        User user = localUser();
        RefreshToken storedToken = refreshTokenEntity(user, LocalDateTime.now().plusDays(1), null);
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash("refresh-ativo")))
                .thenReturn(Optional.of(storedToken));

        MessageResponse response = authService.logout(new LogoutRequest("refresh-ativo"));

        assertThat(response.message()).isNotBlank();
        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void sunnyDay_logoutDeveSerIdempotentePorTokenDesconhecido() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        MessageResponse response = authService.logout(new LogoutRequest("token-nunca-emitido"));

        assertThat(response.message()).isNotBlank();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void sunnyDay_logoutDeveSerIdempotentePorTokenJaRevogado() {
        User user = localUser();
        RefreshToken jaRevogado = refreshTokenEntity(user, LocalDateTime.now().plusDays(1), LocalDateTime.now());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(jaRevogado));

        MessageResponse response = authService.logout(new LogoutRequest("refresh-ja-revogado"));

        assertThat(response.message()).isNotBlank();
        verify(refreshTokenRepository, never()).save(any());
    }

    // ---------- forgotPassword() ----------

    @Test
    void sunnyDay_deveEnviarEmailQuandoEmailExisteEmForgotPassword() {
        User user = localUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

        assertThat(response.message()).isNotBlank();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(user.getEmail()), anyString());
    }

    @Test
    void sunnyDay_forgotPasswordNuncaRevelaSeEmailExiste() {
        when(userRepository.findByEmail("nao-cadastrado@teste.com")).thenReturn(Optional.empty());

        MessageResponse respostaEmailInexistente = authService.forgotPassword(
                new ForgotPasswordRequest("nao-cadastrado@teste.com")
        );

        User user = localUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        MessageResponse respostaEmailExistente = authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

        assertThat(respostaEmailInexistente.message()).isEqualTo(respostaEmailExistente.message());
        verify(emailService, never()).sendPasswordResetEmail(eq("nao-cadastrado@teste.com"), anyString());
    }

    @Test
    void rainyDay_forgotPasswordNaoEmiteTokenParaContaSomenteGoogle() {
        User googleUser = localUser();
        googleUser.setProvider(User.AuthProvider.GOOGLE);
        googleUser.setPassword(null);
        when(userRepository.findByEmail(googleUser.getEmail())).thenReturn(Optional.of(googleUser));

        authService.forgotPassword(new ForgotPasswordRequest(googleUser.getEmail()));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // ---------- resetPassword() ----------

    @Test
    void sunnyDay_deveRedefinirSenhaERevogarRefreshTokensComTokenValido() {
        User user = localUser();
        PasswordResetToken token = passwordResetTokenEntity(user, LocalDateTime.now().plusMinutes(10), null);
        when(passwordResetTokenRepository.findByTokenHash(TokenHasher.hash("token-valido")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nova-senha123")).thenReturn("novo-hash");

        MessageResponse response = authService.resetPassword(new ResetPasswordRequest("token-valido", "nova-senha123"));

        assertThat(response.message()).isNotBlank();
        assertThat(user.getPassword()).isEqualTo("novo-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    void rainyDay_resetPasswordDeveRecusarTokenInexistente() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token-inexistente", "nova-senha123")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rainyDay_resetPasswordDeveRecusarTokenExpirado() {
        User user = localUser();
        PasswordResetToken tokenExpirado = passwordResetTokenEntity(user, LocalDateTime.now().minusMinutes(1), null);
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token-expirado", "nova-senha123")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rainyDay_resetPasswordDeveRecusarTokenJaUsado() {
        User user = localUser();
        PasswordResetToken tokenUsado = passwordResetTokenEntity(user, LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenUsado));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token-usado", "nova-senha123")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(userRepository, never()).save(any());
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
