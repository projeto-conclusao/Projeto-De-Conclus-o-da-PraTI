package com.achadosedevolvidos.support;

import com.achadosedevolvidos.auth.dto.AuthResponse;
import com.achadosedevolvidos.auth.dto.RegisterRequest;
import com.achadosedevolvidos.user.model.User;
import com.achadosedevolvidos.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
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

    protected static final String TEST_MAIL_USERNAME = "auth-teste@localhost";
    protected static final String TEST_MAIL_PASSWORD = "senha-smtp-teste";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    /**
     * Servidor SMTP fake em memória — mesmo papel que o WireMock cumpre para o
     * Google OAuth2: o teste de reset de senha precisa do token BRUTO, que só
     * existe no corpo do e-mail de verdade (no banco só fica o hash), então nada
     * de mockar EmailService — precisamos interceptar o e-mail assíncrono real.
     */
    protected static final GreenMail GREEN_MAIL = new GreenMail(ServerSetupTest.SMTP);

    static {
        POSTGRES.start();
        GREEN_MAIL.setUser(TEST_MAIL_USERNAME, TEST_MAIL_PASSWORD);
        GREEN_MAIL.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // app.jwt.secret não tem valor default no application.yml (ao contrário de
        // GOOGLE_CLIENT_ID/SECRET) — sem isso o contexto Spring nem sobe.
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);

        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
        registry.add("spring.mail.username", () -> TEST_MAIL_USERNAME);
        registry.add("spring.mail.password", () -> TEST_MAIL_PASSWORD);
        // GreenMail (ServerSetupTest.SMTP) não fala STARTTLS — só o Mailtrap real
        // (produção/dev) precisa disso.
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");

        // app.swagger.* também não tem default — mesmo motivo do app.jwt.secret
        // acima.
        registry.add("app.swagger.username", () -> "swagger-teste");
        registry.add("app.swagger.password", () -> "senha-swagger-teste");
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

    /**
     * Espera o e-mail assíncrono de redefinição de senha chegar (via Awaitility,
     * mesmo padrão já usado no projeto para aguardar efeitos de métodos @Async —
     * ver o listener de match) e extrai o token bruto do link no corpo — o único
     * lugar onde ele existe em texto puro, já que o banco só guarda o hash.
     */
    protected String waitForPasswordResetToken(String email) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(findLatestMessageTo(email)).isPresent()
        );

        MimeMessage message = findLatestMessageTo(email).orElseThrow();
        String body = decodedBody(message);

        Matcher matcher = Pattern.compile("token=([^\\s&]+)").matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Token de redefinição não encontrado no corpo do e-mail: " + body);
        }
        return matcher.group(1);
    }

    /**
     * {@link GreenMailUtil#getBody} devolve o conteúdo BRUTO da parte MIME, sem
     * desfazer o Content-Transfer-Encoding — o corpo tem acentuação (ex.: "não",
     * "redefinição"), o que força o JavaMail a codificar a mensagem inteira em
     * quoted-printable, inserindo quebras de linha suaves ("=\r\n") a cada ~76
     * caracteres. Isso corta o link/token ao meio quando lido cru. getContent() do
     * próprio jakarta.mail já decodifica isso corretamente.
     */
    private String decodedBody(MimeMessage message) {
        try {
            return (String) message.getContent();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao ler o corpo do e-mail de teste", e);
        }
    }

    private Optional<MimeMessage> findLatestMessageTo(String email) {
        return Arrays.stream(GREEN_MAIL.getReceivedMessages())
                .filter(message -> enderecadoPara(message, email))
                .reduce((first, second) -> second);
    }

    private boolean enderecadoPara(MimeMessage message, String email) {
        try {
            return message.getAllRecipients() != null && Arrays.stream(message.getAllRecipients())
                    .anyMatch(address -> address.toString().equalsIgnoreCase(email));
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }
}
