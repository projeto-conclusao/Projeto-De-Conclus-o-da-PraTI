package com.achadosedevolvidos.config;

import com.achadosedevolvidos.auth.filter.JwtAuthenticationFilter;
import com.achadosedevolvidos.auth.oauth2.CustomOAuth2UserService;
import com.achadosedevolvidos.auth.oauth2.OAuth2LoginFailureHandler;
import com.achadosedevolvidos.auth.oauth2.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configura os DOIS mecanismos de autenticação lado a lado, propositalmente isolados:
 *
 * <ul>
 *   <li><b>Bearer JWT</b>: stateless, validado por {@link JwtAuthenticationFilter}.
 *   Não depende do Google nem de nenhuma sessão HTTP.</li>
 *   <li><b>OAuth2 Login (Google)</b>: baseado em sessão HTTP (cookie), tratado pelo
 *   próprio {@code oauth2Login()} do Spring Security. Não gera nem depende de JWT.</li>
 * </ul>
 *
 * Se um dos dois mecanismos falhar (ex.: chave JWT inválida, ou Google fora do ar),
 * o outro continua funcionando normalmente — nenhuma requisição autenticada por um
 * mecanismo passa pelo código do outro.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher authEndpoints = new AntPathRequestMatcher("/api/v1/auth/**");
        RequestMatcher bearerRequests = this::isBearerTokenRequest;

        http
                // CSRF continua ATIVO (protege o fluxo baseado em sessão do OAuth2 Login).
                // É dispensado apenas para os endpoints de emissão de token e para
                // requisições que já chegam com um Bearer token — essas não são
                // vulneráveis a CSRF, pois o header Authorization não é enviado
                // automaticamente pelo navegador como um cookie seria.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(authEndpoints, bearerRequests)
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/oauth2/**", "/login/**").permitAll()
                        // Spec OpenAPI + Swagger UI: só documentação, nenhum dado sensível
                        // exposto aqui. Público em dev/local por decisão do time — revisitar
                        // antes de expor este ambiente em produção.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Handshake do WebSocket: a autenticação real acontece no
                        // STOMP CONNECT (StompAuthChannelInterceptor), não aqui.
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        // Regras específicas ANTES da regra genérica de /items/**:
                        // busca e detalhe são públicos, mas matches exige login.
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/*/matches").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/**").permitAll()
                        .requestMatchers("/api/v1/matches/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                // Mecanismo 1 — Bearer JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Mecanismo 2 — OAuth2 Login (Google)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                );

        return http.build();
    }

    private boolean isBearerTokenRequest(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
