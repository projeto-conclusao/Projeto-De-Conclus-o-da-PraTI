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
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

No Google Cloud Console, a **Authorized redirect URI** do client OAuth2 deve ser:
`http://localhost:8080/login/oauth2/code/google` (endpoint padrão do Spring Security,
não o `OAUTH2_REDIRECT_URI` acima — este último é só para onde o *navegador* vai
depois que o backend já concluiu o login).

## Endpoints

```
POST /api/v1/auth/register   { name, email, password }        -> AuthResponse
POST /api/v1/auth/login      { email, password }               -> AuthResponse
POST /api/v1/auth/refresh    { refreshToken }                   -> AuthResponse
GET  /oauth2/authorization/google                                -> inicia login Google
```

`AuthResponse`: `{ accessToken, refreshToken, tokenType: "Bearer", expiresInSeconds }`

## O que o front-end precisa saber

- **Fluxo JWT**: salvar `accessToken`/`refreshToken` (ex.: em memória + um storage
  seguro) e enviar `Authorization: Bearer <accessToken>` em toda chamada à API.
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

O schema (incluindo a tabela `users`) é criado automaticamente pelo Flyway na
subida — `ddl-auto` está em `validate`, o Hibernate só confere se as entidades
batem com o schema já migrado (detalhes em `ARQUITETURA.md`, seção 2).

## Testes incluídos

`src/test/java/.../auth/service/JwtServiceTest.java` cobre geração, validação,
expiração e verificação de subject do token. Rode com `mvn test`.

## Próximos passos sugeridos (fora do escopo deste módulo)

- Rate limiting no `/api/v1/auth/login` (ex.: Bucket4j) para mitigar força bruta.
- Publicar o app OAuth2 no Google Cloud Console (hoje em modo "Testing", só
  e-mails cadastrados como test user conseguem logar via Google).

> A lista completa do que já foi entregue nesta refatoração (incluindo os dois
> itens que saíram desta lista — `AuthenticatedUser` no `ItemController` e
> Flyway) está em `README.md`, seção "O que foi implementado nesta
> refatoração".
