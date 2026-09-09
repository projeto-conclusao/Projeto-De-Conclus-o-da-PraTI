# Módulo de Autenticação e Autorização — Achados e Devolvidos

Este módulo implementa **dois mecanismos de autenticação isolados entre si**,
conforme solicitado: se um cair, o outro continua funcionando normalmente.

| Mecanismo | Como funciona | Onde vive o "login" |
|---|---|---|
| **Bearer Token (JWT)** | `POST /api/v1/auth/register` ou `/login` retornam um `accessToken` + `refreshToken` próprios, assinados com uma chave HS256 da aplicação. | Stateless — o cliente reenvia `Authorization: Bearer <token>` a cada requisição. |
| **OAuth 2.0 (Google)** | `GET /oauth2/authorization/google` inicia o fluxo Authorization Code do Google. Ao concluir, o Spring Security guarda a autenticação na **sessão HTTP (cookie)**. | Baseado em sessão — o navegador reenvia o cookie de sessão automaticamente. |

## Por que essa arquitetura garante isolamento

- O **Bearer JWT** não conhece o Google: `JwtService`, `JwtAuthenticationFilter` e
  `AuthService` não importam nada de `spring-security-oauth2-client`. Se o Google
  estiver fora do ar, ou o `client-id/secret` estiverem errados, o login local e
  todas as chamadas autenticadas por JWT continuam funcionando.
- O **OAuth2 Login** não gera nem valida JWT: `CustomOAuth2UserService` e
  `OAuth2LoginSuccessHandler` não importam `JwtService`. Se a chave JWT
  (`JWT_SECRET`) for rotacionada incorretamente ou o `JwtService` tiver um bug,
  usuários logados via Google continuam autenticados normalmente (a sessão HTTP
  não depende disso).
- Os dois convergem apenas em um ponto conceitual: a entidade `User` e a interface
  `AuthenticatedUser`, que permite que os Controllers escrevam
  `@AuthenticationPrincipal AuthenticatedUser currentUser` sem se importar com qual
  dos dois mecanismos autenticou a requisição.

## Variáveis de ambiente necessárias

```bash
DB_URL=jdbc:postgresql://localhost:5432/achados_e_devolvidos
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Gere com: openssl rand -base64 32
JWT_SECRET=<chave-base64-de-pelo-menos-256-bits>
JWT_ACCESS_EXPIRATION_MS=900000       # 15 min (opcional, já tem default)
JWT_REFRESH_EXPIRATION_MS=604800000   # 7 dias (opcional, já tem default)

# Console do Google Cloud > APIs & Services > Credentials > OAuth Client ID
GOOGLE_CLIENT_ID=<seu-client-id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<seu-client-secret>

OAUTH2_REDIRECT_URI=http://localhost:5173/oauth2/callback
OAUTH2_FAILURE_REDIRECT_URI=http://localhost:5173/login?erro=oauth2

# SMTP - fluxo de esqueci-minha-senha. Defaults apontam para o sandbox do
# Mailtrap (dev/teste, não envia e-mail de verdade). Sem valor default de
# usuário/senha — crie uma inbox grátis em https://mailtrap.io.
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=<usuario-smtp-do-mailtrap>
MAIL_PASSWORD=<senha-smtp-do-mailtrap>
PASSWORD_RESET_REDIRECT_URI=http://localhost:5173/reset-password
PASSWORD_RESET_EXPIRATION_MINUTES=30  # opcional, já tem default

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

No Google Cloud Console, a **Authorized redirect URI** do client OAuth2 deve ser:
`http://localhost:8080/login/oauth2/code/google` (endpoint padrão do Spring Security,
não o `OAUTH2_REDIRECT_URI` acima — este último é só para onde o *navegador* vai
depois que o backend já concluiu o login).

## Endpoints

Todos em `/api/v1/auth/*`, exceto o login Google (`/oauth2/...`, fora do prefixo
`/api/v1` por ser o fluxo padrão do Spring Security).

| Método | Path | Body | Resposta | Autenticação | Ação |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `{ name, email, password }` | `201` `AuthResponse` | Nenhuma | Cria a conta e já devolve o par de tokens (login automático). |
| `POST` | `/api/v1/auth/login` | `{ email, password }` | `200` `AuthResponse` | Nenhuma | Autentica e-mail/senha e devolve o par de tokens. |
| `POST` | `/api/v1/auth/refresh` | `{ refreshToken }` | `200` `AuthResponse` | Nenhuma (o `refreshToken` no body é a credencial) | Renova o `accessToken`. **Rotaciona**: o `refreshToken` usado é invalidado e um novo é devolvido — ver aviso abaixo. |
| `POST` | `/api/v1/auth/logout` | `{ refreshToken }` | `200` `MessageResponse` | Nenhuma | Invalida o `refreshToken` no servidor. Sempre `200`, mesmo com token já inválido/inexistente (idempotente). |
| `GET` | `/oauth2/authorization/google` | — | Redirect | Nenhuma | Inicia o login social com Google (fluxo de sessão/cookie, não devolve JWT). |
| `POST` | `/api/v1/auth/forgot-password` | `{ email }` | `200` `MessageResponse` | Nenhuma | Se o e-mail existir e não for conta só-Google, envia por e-mail um link com token de redefinição. **Sempre** devolve a mesma mensagem genérica, exista ou não o e-mail (não dá pra usar a resposta para checar se um e-mail está cadastrado). |
| `POST` | `/api/v1/auth/reset-password` | `{ token, newPassword }` | `200` `MessageResponse` | Nenhuma (o `token` recebido por e-mail é a credencial) | Define a nova senha e **revoga todos os refresh tokens da conta** — qualquer sessão ativa (nesse ou outro dispositivo) precisa logar de novo. |

`AuthResponse`: `{ accessToken, refreshToken, tokenType: "Bearer", expiresInSeconds }`

`MessageResponse`: `{ message: string }` — corpo padrão dos 3 endpoints novos, sempre com uma mensagem amigável em português, nunca um objeto de erro estruturado (esse é o `ApiError` do `GlobalExceptionHandler`, usado só nas respostas de erro — `400`/`401`).

Erros seguem o padrão já existente do resto da API: `ApiError { status, message }`. Casos relevantes:
- `refresh`/`reset-password` com token inválido, expirado ou já usado → `401`, mesma mensagem genérica em ambos ("Refresh token inválido ou expirado" / "Token de redefinição inválido ou expirado") — de propósito, para não dar pista a quem estiver tentando adivinhar tokens.
- `reset-password` com `newPassword` de menos de 8 caracteres → `400` (validação de payload, igual ao `register`).

## O que o front-end precisa saber

- **Fluxo JWT**: salvar `accessToken`/`refreshToken` (ex.: em memória + um storage
  seguro) e enviar `Authorization: Bearer <accessToken>` em toda chamada à API.
- **⚠️ Rotação de refresh token**: toda vez que `/auth/refresh` é chamado, o
  `refreshToken` enviado deixa de funcionar — o novo `refreshToken` que vem na
  resposta é quem passa a valer. **Sempre sobrescreva o `refreshToken` salvo com o
  que voltou de cada chamada a `/refresh`**, nunca reaproveite o antigo. Chamar
  `/refresh` duas vezes com o mesmo token (ex.: duas abas fazendo refresh ao mesmo
  tempo) faz a segunda chamada falhar com `401`.
- **Logout é "fire and forget"**: `/auth/logout` sempre responde `200`, então não
  precisa tratar erro nele — só limpar os tokens salvos no cliente depois de
  chamar (ou mesmo antes, já que o servidor nunca recusa).
- **Forgot-password nunca revela se o e-mail existe**: a UI deve mostrar a mesma
  mensagem de sucesso genérica sempre que o endpoint responder `200`, independente
  do e-mail digitado existir ou não. Não construa uma tela de "e-mail não
  encontrado" a partir da resposta desse endpoint — a API nunca vai diferenciar
  isso por design (evita que alguém descubra quais e-mails estão cadastrados
  testando um por um).
- **Reset-password desloga todos os dispositivos**: depois de um reset bem
  sucedido, qualquer `accessToken`/`refreshToken` emitido antes deixa de
  funcionar — inclusive o da própria aba que iniciou o fluxo, se ela estava
  logada. Espere ter que redirecionar para a tela de login após o reset.
- **Fluxo Google**: redirecionar o navegador para
  `http://localhost:8080/oauth2/authorization/google`. Ao final, o backend
  redireciona para `OAUTH2_REDIRECT_URI`. A partir daí, as chamadas à API precisam
  ir com `credentials: 'include'` (fetch) ou `withCredentials: true` (axios) para
  que o cookie de sessão seja enviado.
- **CSRF no fluxo Google**: como esse fluxo usa cookie de sessão, requisições
  `POST/PUT/PATCH/DELETE` feitas por um usuário logado via Google precisam do
  header `X-XSRF-TOKEN`, com o valor lido do cookie `XSRF-TOKEN` que o backend
  envia. Requisições autenticadas via Bearer JWT **não** precisam disso.

## Como rodar

Ver `README.md` (seções 1 e 2) para o passo a passo completo — instalação de
Java/Maven/Docker, `.env` e `docker compose up -d`. Resumo, já com tudo
instalado e configurado:

```bash
docker compose up -d
set -a && source .env && set +a && mvn spring-boot:run
```

O schema (incluindo as tabelas `users`, `refresh_tokens` e
`password_reset_tokens`) é criado automaticamente pelo Flyway na subida —
`ddl-auto` está em `validate`, o Hibernate só confere se as entidades batem com
o schema já migrado (detalhes em `ARQUITETURA.md`, seção 2).

## Testes incluídos

- `auth/service/JwtServiceTest.java` — geração, validação, expiração e
  verificação de subject do token. Unitário, sem Spring/banco.
- `auth/service/AuthServiceTest.java` — todos os fluxos (register, login,
  refresh com rotação, logout, forgot-password, reset-password), caminho feliz
  e principais erros de cada um. Unitário (Mockito), sem Spring/banco.
- `auth/controller/AuthControllerIT.java` — os mesmos fluxos de ponta a ponta
  (HTTP real → Postgres real via Testcontainers). O e-mail de redefinição de
  senha é capturado de verdade por um servidor SMTP fake em memória (GreenMail,
  configurado em `support/IntegrationTestSupport.java`) — o teste extrai o
  token do link recebido, nunca lê direto do banco (lá só existe o hash).

```bash
mvn test                             # unitários, rápido, sem Docker
mvn verify -Dskip.unit.tests=true    # integração, precisa do Docker rodando
```

## Próximos passos sugeridos (fora do escopo deste módulo)

- Rate limiting no `/api/v1/auth/login` (ex.: Bucket4j) para mitigar força bruta.
- Publicar o app OAuth2 no Google Cloud Console (hoje em modo "Testing", só
  e-mails cadastrados como test user conseguem logar via Google).
- Job de limpeza para linhas expiradas/revogadas em `refresh_tokens` e
  `password_reset_tokens` — hoje elas só se acumulam, sem impacto perceptível
  na escala atual, mas crescimento sem limite eventualmente vai pedir isso.

> A lista completa do que já foi entregue nesta refatoração (incluindo os dois
> itens que saíram desta lista — `AuthenticatedUser` no `ItemController` e
> Flyway) está em `README.md`, seção "O que foi implementado nesta
> refatoração".
