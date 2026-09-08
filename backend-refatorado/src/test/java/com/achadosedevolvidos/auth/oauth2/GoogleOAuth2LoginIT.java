package com.achadosedevolvidos.auth.oauth2;

import com.achadosedevolvidos.support.IntegrationTestSupport;
import com.achadosedevolvidos.user.model.User;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integração do fluxo OAuth2 Login (Google) descrito em README-AUTH.md. Como o
 * backend faz chamadas HTTP reais ao provedor durante o callback
 * ({@code /login/oauth2/code/google}) — token-uri e user-info-uri — este teste
 * aponta essas duas URLs (via {@code @DynamicPropertySource}) para um WireMock
 * local, simulando as respostas do Google sem depender de credenciais nem rede
 * externa reais. A authorization-uri não precisa ser mockada: o teste nunca
 * navega até lá, só reaproveita o {@code state} que o próprio backend gera no
 * redirect de {@code /oauth2/authorization/google}.
 *
 * <p>A sessão HTTP precisa ser reaproveitada entre as duas chamadas (autorização
 * -> callback), porque é nela que o Spring Security guarda o
 * {@code OAuth2AuthorizationRequest} pendente entre as duas etapas — exatamente
 * como um navegador real faria via cookie de sessão.
 */
class GoogleOAuth2LoginIT extends IntegrationTestSupport {

    private static final WireMockServer GOOGLE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        GOOGLE_MOCK.start();
    }

    @DynamicPropertySource
    static void googleProviderProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.provider.google.token-uri",
                () -> GOOGLE_MOCK.baseUrl() + "/token");
        registry.add("spring.security.oauth2.client.provider.google.user-info-uri",
                () -> GOOGLE_MOCK.baseUrl() + "/userinfo");
    }

    @AfterAll
    static void stopWireMock() {
        GOOGLE_MOCK.stop();
    }

    @AfterEach
    void resetStubs() {
        GOOGLE_MOCK.resetAll();
    }

    @Test
    void sunnyDay_deveCriarUsuarioNovoNoPrimeiroLoginComGoogle() throws Exception {
        String email = "novo-google-" + UUID.randomUUID() + "@teste.com";
        stubTokenExchange();
        stubUserInfo("""
                {"sub": "1234567890", "email": "%s", "name": "Usuária Google"}
                """.formatted(email));

        String redirect = simulateGoogleCallback("codigo-valido");

        assertThat(redirect).startsWith("http://localhost:5173/oauth2/callback");
        assertThat(redirect).contains("login=success");

        User criado = userRepository.findByEmail(email).orElseThrow();
        assertThat(criado.getProvider()).isEqualTo(User.AuthProvider.GOOGLE);
        assertThat(criado.getName()).isEqualTo("Usuária Google");
    }

    @Test
    void sunnyDay_contaLocalExistenteDevePassarAAceitarLoginGoogleTambem() throws Exception {
        AuthenticatedTestUser contaLocal = registerAndAuthenticate("Conta Local");
        stubTokenExchange();
        stubUserInfo("""
                {"sub": "9999999999", "email": "%s", "name": "%s"}
                """.formatted(contaLocal.user().getEmail(), contaLocal.user().getName()));

        String redirect = simulateGoogleCallback("codigo-valido");

        assertThat(redirect).contains("login=success");

        User atualizado = userRepository.findByEmail(contaLocal.user().getEmail()).orElseThrow();
        assertThat(atualizado.getProvider()).isEqualTo(User.AuthProvider.LOCAL_AND_GOOGLE);
        // A senha local não pode ser apagada por causa do login via Google.
        assertThat(atualizado.getPassword()).isNotBlank();
    }

    @Test
    void rainyDay_deveRedirecionarParaFalhaQuandoGoogleNaoRetornaEmail() throws Exception {
        stubTokenExchange();
        stubUserInfo("""
                {"sub": "1111111111", "name": "Sem Email"}
                """);

        String redirect = simulateGoogleCallback("codigo-valido");

        assertThat(redirect).startsWith("http://localhost:5173/login");
        assertThat(redirect).contains("error=oauth2_failed");
    }

    @Test
    void rainyDay_deveRedirecionarParaFalhaQuandoProvedorRecusaOCodigo() throws Exception {
        GOOGLE_MOCK.stubFor(WireMock.post(WireMock.urlPathEqualTo("/token"))
                .willReturn(WireMock.aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_grant\"}")));

        String redirect = simulateGoogleCallback("codigo-invalido");

        assertThat(redirect).startsWith("http://localhost:5173/login");
        assertThat(redirect).contains("error=oauth2_failed");
    }

    private void stubTokenExchange() {
        GOOGLE_MOCK.stubFor(WireMock.post(WireMock.urlPathEqualTo("/token"))
                .willReturn(WireMock.okJson("""
                        {"access_token": "token-de-teste", "token_type": "Bearer", "expires_in": 3600, "scope": "email profile"}
                        """)));
    }

    private void stubUserInfo(String body) {
        GOOGLE_MOCK.stubFor(WireMock.get(WireMock.urlPathEqualTo("/userinfo"))
                .willReturn(WireMock.okJson(body)));
    }

    /**
     * Dispara {@code GET /oauth2/authorization/google} (o backend gera e guarda na
     * sessão um {@code state} + {@code redirect_uri}), extrai o {@code state} do
     * redirect retornado e então chama o callback
     * {@code GET /login/oauth2/code/google} reaproveitando a mesma sessão — como um
     * navegador real faria ao voltar do Google. Devolve a URL final de redirect
     * (sucesso ou falha, conforme os stubs configurados no WireMock).
     */
    private String simulateGoogleCallback(String authorizationCode) throws Exception {
        MvcResult authorizationResult = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) authorizationResult.getRequest().getSession(false);
        // O Location vem com o "state" percent-encoded (ex.: "%3D" no lugar de
        // "="). UriComponents.getQueryParams() devolve o valor cru, ainda
        // codificado — sem decodificar manualmente aqui, o valor reenviado no
        // callback nunca bateria com o state decodificado guardado na sessão, e o
        // Spring Security rejeitaria com "invalid_state_parameter" antes de sequer
        // tentar trocar o code pelo token (falha instantânea, sem nenhuma chamada
        // ao WireMock).
        String rawState = UriComponentsBuilder.fromUriString(authorizationResult.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("state");
        String state = URLDecoder.decode(rawState, StandardCharsets.UTF_8);

        MvcResult callbackResult = mockMvc.perform(get("/login/oauth2/code/google")
                        .session(session)
                        .param("code", authorizationCode)
                        .param("state", state))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return callbackResult.getResponse().getRedirectedUrl();
    }
}
