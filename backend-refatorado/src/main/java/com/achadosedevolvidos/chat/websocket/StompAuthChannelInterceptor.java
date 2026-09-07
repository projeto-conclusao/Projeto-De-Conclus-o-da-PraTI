package com.achadosedevolvidos.chat.websocket;

import com.achadosedevolvidos.auth.service.JwtService;
import com.achadosedevolvidos.shared.exception.AppException;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * O protótipo original do ChatController não tinha NENHUMA autenticação: qualquer
 * um podia se conectar ao WebSocket e mandar mensagem em nome de qualquer usuário.
 * Este interceptor exige um "Authorization: Bearer <jwt>" como header nativo do
 * frame STOMP CONNECT, valida com o mesmo {@link JwtService} usado pelo mecanismo
 * Bearer da API REST (sem duplicar a lógica de validação), e associa um
 * {@link StompPrincipal} — com o ID do usuário, não o e-mail — à sessão.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        }

        return message;
    }

    private StompPrincipal authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AppException("Token ausente na conexão WebSocket", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            String email = jwtService.extractUsername(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException("Usuário do token não encontrado", HttpStatus.UNAUTHORIZED));

            if (!jwtService.isTokenValid(token, user)) {
                throw new AppException("Token expirado ou inválido na conexão WebSocket", HttpStatus.UNAUTHORIZED);
            }

            return new StompPrincipal(user.getId().toString());
        } catch (JwtException | IllegalArgumentException e) {
            throw new AppException("Token inválido na conexão WebSocket", HttpStatus.UNAUTHORIZED);
        }
    }
}
