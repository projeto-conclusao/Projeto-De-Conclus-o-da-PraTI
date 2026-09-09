package com.achadosedevolvidos.support;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para todos os testes de integração (sufixo *IT). Sobe um Postgres real via
 * Testcontainers — nada de banco em memória — e roda o contexto Spring completo
 * numa porta real ({@code WebEnvironment.RANDOM_PORT}), necessário para os testes
 * de WebSocket/STOMP que precisam de uma conexão de verdade, não só do
 * MockMvc.
 *
 * <p>O container é iniciado uma única vez (bloco estático, nunca parado
 * explicitamente — o Ryuk do Testcontainers cuida da limpeza ao fim da JVM) e
 * compartilhado por todas as subclasses: como a configuração de contexto Spring é
 * idêntica entre elas, o Spring Test reaproveita o mesmo ApplicationContext, então
 * o Postgres só sobe e as migrations do Flyway só rodam uma vez por execução de
 * {@code mvn verify}, não uma vez por classe de teste.
 *
 * <p>Propositalmente NÃO há {@code @Transactional} aqui: o fluxo de match usa um
 * listener {@code @TransactionalEventListener(phase = AFTER_COMMIT)}, que nunca
 * dispararia se cada teste rodasse dentro de uma transação que o Spring Test
 * sempre desfaz ao final (nunca chega a commitar de verdade). Por isso os dados
 * de um teste persistem para os próximos — cada teste usa e-mails/títulos únicos
 * (via UUID) para não colidir com dados de outros testes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    protected static final String TEST_JWT_SECRET =
            Base64.getEncoder().encodeToString("chave-de-teste-de-integracao-com-256-bits-no-minimo!!!".getBytes());

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // app.jwt.secret não tem valor default no application.yml (ao contrário de
        // GOOGLE_CLIENT_ID/SECRET) — sem isso o contexto Spring nem sobe.
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @LocalServerPort
    protected int port;

    protected record AuthenticatedTestUser(User user, String accessToken) {

        public String authorizationHeader() {
            return "Bearer " + accessToken;
        }
    }

    /**
     * Registra um usuário novo (e-mail único por chamada) através do endpoint real
     * de cadastro — em vez de inserir direto no repositório — para que os testes
     * de outras features já partam de um usuário autenticado do jeito que um
     * cliente real obteria.
     */
    protected AuthenticatedTestUser registerAndAuthenticate(String namePrefix) throws Exception {
        String emailLocalPart = namePrefix.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        String email = emailLocalPart + "-" + UUID.randomUUID() + "@teste.com";
        String password = "senha12345";

        RegisterRequest request = new RegisterRequest(namePrefix, email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class
        );
        User user = userRepository.findByEmail(email).orElseThrow();

        return new AuthenticatedTestUser(user, authResponse.accessToken());
    }
}
