# Achados e Devolvidos — API (back-end)

> **Este diretório é o resultado de uma refatoração completa do back-end original
> do projeto** (antigo diretório `back-end/`, com controllers/entidades soltos e
> sem gerência de schema). O código antigo foi removido do repositório; este é o
> único back-end ativo a partir de agora. Veja a seção
> [O que foi implementado nesta refatoração](#o-que-foi-implementado-nesta-refatoração)
> para o detalhamento completo, e `ARQUITETURA.md` para a justificativa técnica de
> cada mudança.

Documentação relacionada:

- **`ARQUITETURA.md`** — o que mudou no refactor e por quê (desacoplamento,
  Flyway, estrutura de pacotes).
- **`README-AUTH.md`** — detalhes dos dois mecanismos de autenticação
  (Bearer JWT e OAuth2/Google): endpoints, contratos, o que o front-end precisa
  enviar/receber.
- **`README-OPENAPI.md`** — como gerar tipos TypeScript automaticamente a
  partir da API real (Swagger UI + `openapi-typescript`/`orval`).

## Stack

Java 21 · Spring Boot 3.3 · Spring Security 6 (JWT + OAuth2 Client) ·
Spring Data JPA · Hibernate 6 · PostgreSQL 16 · Flyway · WebSocket/STOMP ·
Maven · Lombok · JJWT 0.12 · JUnit 5

---

## 1. Pré-requisitos: instalação do zero

Esta seção assume uma máquina limpa. Se você já tem alguma ferramenta instalada,
pule para a próxima e apenas confira a versão com o comando de verificação.

### 1.1. Java 21 (JDK)

O projeto usa **Java 21** (LTS). Qualquer distribuição de JDK 21 funciona
(Amazon Corretto, Eclipse Temurin, Oracle JDK); os exemplos abaixo usam o
Corretto.

**Windows:**

1. Baixe o instalador MSI em https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html
   (ou use `winget install Amazon.Corretto.21.JDK` no PowerShell).
2. Rode o instalador — ele já configura `JAVA_HOME` e o `PATH` automaticamente.
3. Abra um novo terminal e confirme:
   ```powershell
   java -version
   ```
   Deve mostrar `openjdk version "21..."`.

**macOS:**

```bash
brew install --cask corretto21
```

**Linux (Debian/Ubuntu):**

```bash
sudo apt update
sudo apt install java-21-amazon-corretto-jdk
```

Em qualquer SO, se você gerencia múltiplas versões de Java, `sdkman` é uma
alternativa prática: `sdk install java 21.0.5-amzn`.

### 1.2. Apache Maven 3.10.0-rc-1

> **Nota:** `3.10.0-rc-1` é um release candidate. Ele não fica hospedado no
> mirror padrão de downloads (que só lista releases estáveis), mas sim no
> repositório de desenvolvimento da Apache. Se seu ambiente exigir
> especificamente essa versão (ex.: para bater com o que já está instalado na
> máquina de outros membros do time), baixe o binário em
> https://archive.apache.org/dist/maven/maven-3/3.10.0/binaries/ (arquivo
> `apache-maven-3.10.0-rc-1-bin.zip`, se disponível no momento do download) ou
> peça o `.zip` para quem já tem. Caso não esteja mais acessível, qualquer
> Maven **3.9.x** estável funciona sem nenhuma alteração no `pom.xml`.

**Windows:**

1. Extraia o `.zip` em um diretório fixo, ex.: `C:\apache-maven-3.10.0`.
2. Configure as variáveis de ambiente (PowerShell como Administrador, ou via
   "Editar variáveis de ambiente do sistema" na UI):
   ```powershell
   [Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\apache-maven-3.10.0\apache-maven-3.10.0-rc-1", "User")
   [Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\apache-maven-3.10.0\apache-maven-3.10.0-rc-1\bin", "User")
   ```
3. Abra um novo terminal e confirme:
   ```powershell
   mvn -version
   ```
   Deve mostrar `Apache Maven 3.10.0-rc-1` e o `Java version: 21...` detectado
   acima.

**macOS/Linux:**

```bash
curl -O https://archive.apache.org/dist/maven/maven-3/3.10.0/binaries/apache-maven-3.10.0-rc-1-bin.tar.gz
tar -xzf apache-maven-3.10.0-rc-1-bin.tar.gz -C /opt
echo 'export PATH="/opt/apache-maven-3.10.0-rc-1/bin:$PATH"' >> ~/.bashrc   # ou ~/.zshrc
source ~/.bashrc
mvn -version
```

### 1.3. Docker (para o banco de dados)

O Postgres roda em container — você não precisa instalar Postgres na máquina.

**Windows:**

1. Instale o **Docker Desktop**: https://www.docker.com/products/docker-desktop/
   (requer WSL2; o instalador guia essa configuração se ainda não estiver
   habilitada).
2. Abra o Docker Desktop pelo menos uma vez e espere o ícone da baleia
   indicar "Engine running".
3. Confirme no terminal:
   ```powershell
   docker --version
   docker compose version
   ```

**macOS:** Docker Desktop, mesmo link acima.

**Linux:** instale `docker-ce` + o plugin `docker-compose-plugin` conforme a
distribuição (https://docs.docker.com/engine/install/) e adicione seu usuário
ao grupo `docker` para não precisar de `sudo` em todo comando.

---

## 2. Configurando o projeto localmente

### 2.1. Variáveis de ambiente — cuidado com dados sensíveis

```bash
cd backend-refatorado
cp .env.example .env
```

O `.env` concentra **tudo que é sensível ou específico do seu ambiente**:
credenciais de banco, chave de assinatura JWT, client secret do Google. Regras
que valem para qualquer pessoa do time:

- **`.env` nunca é commitado.** Já está no `.gitignore` da raiz do repositório
  (regra `.env`). Só o `.env.example` (com campos vazios/valores de exemplo)
  fica versionado.
- **Nunca cole valores reais de `JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, senha de
  banco de produção, etc. em PR, issue, chat da equipe ou neste README.** Se
  um segredo vazar (ex.: colado por engano em um canal público), rotacione-o
  imediatamente — gerar um novo client secret no Google Cloud Console ou um
  novo `JWT_SECRET` é gratuito e não tem downtime além de invalidar sessões/
  tokens ativos.
- Gere o `JWT_SECRET` localmente, nunca reaproveite o de outro ambiente:
  ```bash
  openssl rand -base64 32
  ```
- Em produção, essas variáveis devem vir de um cofre de segredos gerenciado
  pela infraestrutura (ex.: variáveis de ambiente do serviço de deploy,
  AWS Secrets Manager, etc.) — nunca de um arquivo `.env` dentro do container
  ou da imagem publicada.

Preencha no `.env`:

| Variável | Obrigatória | Descrição |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Sim | Credenciais do container Postgres (usadas pelo `docker-compose.yml`) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Sim | Como a aplicação Spring se conecta ao mesmo Postgres acima |
| `JWT_SECRET` | Sim | Chave HS256 (Base64, ≥ 256 bits) para assinar os tokens Bearer. Sem valor padrão — a aplicação **não sobe** sem isso. |
| `JWT_ACCESS_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS` | Não | Têm default (15 min / 7 dias) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Só para testar login Google | Sem essas credenciais reais, o login local (JWT) funciona normalmente; só o botão "Entrar com Google" falha. Veja `README-AUTH.md` para o passo a passo de criação no Google Cloud Console. |
| `OAUTH2_REDIRECT_URI` / `OAUTH2_FAILURE_REDIRECT_URI` | Não | Para onde o navegador volta após o login Google (sucesso/falha) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Só para testar "esqueci minha senha" | Credenciais SMTP (ex.: Mailtrap). Sem valor padrão — sem elas, a aplicação **não sobe**. `MAIL_HOST`/`MAIL_PORT` têm default (sandbox do Mailtrap). Veja `README-AUTH.md`. |
| `PASSWORD_RESET_REDIRECT_URI` / `PASSWORD_RESET_EXPIRATION_MINUTES` | Não | Página do front-end para onde aponta o link de redefinição, e validade do token (têm default) |
| `SWAGGER_USERNAME` / `SWAGGER_PASSWORD` | Sim | Credencial (Basic Auth) para abrir `/swagger-ui` e `/v3/api-docs` — combine com o time (frontend + backend), não é conta de usuário do app. Sem valor padrão — a aplicação **não sobe** sem isso. Veja `README-OPENAPI.md`. |
| `CORS_ALLOWED_ORIGINS` | Não | Origem(ns) do front-end permitidas via CORS |

### 2.2. Subindo o banco de dados

```bash
docker compose up -d
docker compose ps    # confirma o container "achados-postgres" como Up
```

Os dados persistem no volume nomeado `achados-postgres-data` — reiniciar o
container (ou a máquina) não apaga nada. Para resetar o banco do zero:
`docker compose down -v` (isso **apaga** o volume e todos os dados).

### 2.3. Compilando

```bash
mvn compile
```

### 2.4. Rodando os testes

Há duas suítes, separadas por convenção de nome e por plugin Maven — rodar uma
não repete a outra:

```bash
# Unitários (classes *Test.java, via Surefire) — rápidos, sem Docker.
mvn test

# Integração (classes *IT.java, via Failsafe) — sobem um Postgres real via
# Testcontainers e simulam o Google via WireMock. Precisa do Docker rodando
# (seção 1.3). "mvn verify" roda os unitários e os de integração juntos; para
# rodar só os de integração:
mvn verify -Dskip.unit.tests=true
```

**Unitários** (Mockito, sem Spring, sem banco): `JwtService`, `AuthService`,
`ItemServiceImpl`, `ChatServiceImpl` e `MatchEngineService` — cobrindo o
caminho feliz e os principais erros de cada um (e-mail duplicado, credenciais
inválidas, usuário/categoria/match inexistente, remetente que não participa da
conversa, etc.).

**Integração** (`@SpringBootTest` + Postgres real via Testcontainers):
endpoints REST de auth/itens/categorias/chat, o fluxo assíncrono completo
`item criado → evento → motor de match → match persistido`
(`MatchFlowIT`), a conexão WebSocket/STOMP autenticada do chat
(`ChatWebSocketIT`), e o callback do login Google com o provedor simulado via
WireMock (`GoogleOAuth2LoginIT`) — cobrindo tanto o cadastro de usuário novo
quanto a vinculação de uma conta local já existente, e as falhas do provedor
(sem e-mail no retorno, `code` recusado).

### 2.5. Rodando a aplicação

```bash
set -a && source .env && set +a && mvn spring-boot:run
```

No **IntelliJ**, a Run Configuration da classe `AchadosEDevolvidosApplication`
pode carregar o `.env` diretamente em "Modify options → Environment
variables" (IntelliJ 2023.1+ tem suporte nativo a "Environment file") — assim
você não precisa exportar as variáveis manualmente a cada execução pelo
terminal.

O schema é criado/atualizado automaticamente pelo Flyway na subida (ver
`ARQUITETURA.md`, seção 2). Se tudo estiver certo, o log termina com
`Started AchadosEDevolvidosApplication` e a API responde em
`http://localhost:8080`.

---

## 3. Integração Contínua (GitHub Actions)

Pipeline em `.github/workflows/backend-ci.yml`, disparada em push/PR para
`main`/`backend-develop` (só quando algo em `backend-refatorado/` muda), com
runner `ubuntu-latest`:

| Job | O que faz |
|---|---|
| `build` | `mvn compile` — falha rápido se não compilar, antes de gastar tempo com testes. |
| `unit-tests` | `mvn test` (Surefire). Publica os resultados como Check Run e como artefato (`surefire-reports`). |
| `integration-tests` | `mvn verify -Dskip.unit.tests=true` (Failsafe). O runner Ubuntu já tem Docker nativamente — Testcontainers e WireMock sobem sem nenhum setup extra. Publica os resultados (`failsafe-reports`). |
| `ci-status` | Gate único que só passa se os dois jobs de teste passarem — configure só ele como *required check* na proteção da branch, em vez de dois. |

`unit-tests` e `integration-tests` rodam em paralelo (ambos dependem só de
`build`), não um depois do outro — o tempo total do pipeline é
aproximadamente o do mais lento dos dois, não a soma.

---

## O que foi implementado nesta refatoração

Resumo funcional — a justificativa técnica de cada item está em
`ARQUITETURA.md`.

- **Reorganização por feature**: um único pacote raiz
  `com.achadosedevolvidos`, dividido em `auth`, `user`, `category`, `item`,
  `match`, `chat`, `config` e `shared`, eliminando a duplicidade de
  controllers/entidades do protótipo original.
- **Schema de banco versionado via Flyway** (`V1` a `V11`, em
  `src/main/resources/db/migration`), com `ddl-auto: validate` — o Hibernate
  deixou de criar/alterar tabelas por conta própria.
- **Autenticação dupla e isolada** (detalhes em `README-AUTH.md`):
  - Bearer JWT stateless (`/api/v1/auth/register|login|refresh`), com
    `JwtService` e `JwtAuthenticationFilter` próprios.
  - OAuth2 Login via Google (`/oauth2/authorization/google`), baseado em
    sessão HTTP, com find-or-create de usuário por e-mail
    (`CustomOAuth2UserService`).
  - Os dois mecanismos não têm dependência de código um com o outro — uma
    falha em um não derruba o outro. Ambos convergem só na interface
    `AuthenticatedUser`, usada pelos Controllers via
    `@AuthenticationPrincipal`.
- **`ItemController` corrigido**: antes lia `@RequestAttribute("userId")`
  (nunca populado por nenhum filtro); agora usa
  `@AuthenticationPrincipal AuthenticatedUser`, funcionando com os dois
  mecanismos de login.
- **WebSocket do chat autenticado**: `StompAuthChannelInterceptor` passou a
  exigir e validar um Bearer JWT no `CONNECT` do STOMP; o remetente da
  mensagem vem do `Principal` da sessão autenticada, nunca de um campo
  enviado pelo cliente. `ChatServiceImpl` valida que o remetente é de fato um
  dos dois donos de item do match.
- **Desacoplamento `item` ↔ `match` via evento**: `ItemServiceImpl` publica
  um `ItemCreatedEvent` (não conhece o módulo `match`); um listener
  `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` dispara o
  motor de match numa thread separada, sem afetar a resposta HTTP de criação
  do item e sem propagar erros do motor de match.
- **Motor de match (`MatchEngineService`) puro e testável**: opera sobre um
  `record` (`MatchCandidate`), sem dependência de JPA — testado com
  `new MatchEngineService()`, sem subir contexto Spring (`MatchEngineServiceTest`).
- **Busca dinâmica de itens** (`GET /api/v1/items/search`) via Spring Data
  JPA Specifications, com cada filtro isolado e combinável em
  `ItemSpecifications`.
- **`BaseEntity`** centraliza `id` (UUID) e `createdAt` (`@PrePersist`) — as
  entidades e services deixaram de redeclarar/preencher isso manualmente.
- **Camadas com contrato explícito**: Controller → Service (interface) →
  Repository em todo fluxo de escrita (`ItemService`, `MatchService`,
  `ChatService`), permitindo trocar implementação ou mockar em teste sem
  tocar no Controller.
- **Ambiente de desenvolvimento reprodutível**: `docker-compose.yml` (Postgres
  com volume nomeado, dados persistentes) + `.env.example` como template —
  ver seção 2 acima.
- **Suíte de testes unitários e de integração** (ver seção 2.4) — os de
  integração usam Postgres real via Testcontainers e simulam o Google via
  WireMock, cobrindo autenticação (JWT e OAuth2/Google), itens, categorias,
  chat e o fluxo assíncrono completo de match.
- **Pipeline de CI no GitHub Actions** (ver seção 3) — compila e roda as duas
  suítes de teste a cada push/PR.
- **Campo `shortDescription` em `Item`** (migrations `V10`/`V11`): itens
  passaram a ter uma descrição curta (`short_description`, até 100
  caracteres, obrigatória) usada em listagens, além da descrição completa
  (`description`), que também se tornou obrigatória — antes aceitava `NULL`.
- **3 bugs de correção encontrados e corrigidos ao escrever os testes de
  integração** (nenhum coberto antes, porque testes com mocks não exercitam
  proxies do Hibernate nem o ciclo de vida real de uma transação):
  - `AuthService.refreshToken`: um refresh token malformado ou expirado
    escapava como `500` em vez do `401` pretendido pelo código (a exceção do
    JJWT não era capturada antes de chegar no handler genérico de erro).
  - `GlobalExceptionHandler`: qualquer rota inexistente (nenhum
    `@Controller` nem recurso estático correspondente) também virava `500`
    em vez de `404`, pelo mesmo motivo — o handler genérico de `Exception`
    interceptava a `NoResourceFoundException` do Spring antes dela virar o
    404 que já carregava por padrão.
  - `ItemServiceImpl.findById/search`, `MatchServiceImpl.findMatchesForItem`
    e `ChatServiceImpl.history`: como `spring.jpa.open-in-view` está
    desligado (decisão deliberada do projeto), esses quatro métodos de
    leitura lançavam `LazyInitializationException` (→ 500) ao tentar
    acessar relações `@ManyToOne` preguiçosas (`item.getUser()`,
    `match.getLostItem()`, etc.) fora de uma transação. Faltava
    `@Transactional(readOnly = true)` nos quatro.

### Fora do escopo desta refatoração (próximos passos conhecidos)

- Rate limiting no `/api/v1/auth/login`.
- Busca geográfica por raio (PostGIS ou Haversine em SQL nativo).
- Paginação em `/api/v1/items/search`.
- Publicação do app OAuth2 no Google (hoje em modo "Testing", só e-mails
  cadastrados como test user conseguem logar via Google).

---

## Principais endpoints

```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
GET  /oauth2/authorization/google

GET  /api/v1/categories

POST /api/v1/items
GET  /api/v1/items/search?type=&categoryId=&query=
GET  /api/v1/items/{id}
GET  /api/v1/items/{id}/matches

GET  /api/v1/matches/{matchId}/messages        (histórico do chat)
WS   /ws  →  SEND /app/chat.sendMessage/{matchId}
             SUBSCRIBE /topic/conversation/{matchId}
```

## Estrutura de pacotes

```
com.achadosedevolvidos
├── auth/          # Bearer JWT: controller, service, filtro, DTOs
│   └── oauth2/    # OAuth2 Login (Google): isolado do JWT
├── user/          # User + AuthenticatedUser (contrato comum aos dois logins)
├── category/      # Categoria (entidade simples, CRUD de leitura)
├── item/          # Item, ItemImage, busca, DTOs, evento de criação
├── match/         # Match, motor de pontuação puro, orquestração
├── chat/          # Mensagens por match, WebSocket autenticado
├── config/        # Security, WebSocket, Async, beans de autenticação
└── shared/        # BaseEntity, exceções e resposta de erro padrão
```

Detalhes de cada decisão de arquitetura em `ARQUITETURA.md`.
