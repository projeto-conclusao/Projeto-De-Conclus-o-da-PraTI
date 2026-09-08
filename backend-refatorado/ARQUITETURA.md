# Arquitetura — Achados e Devolvidos (back-end refatorado)

Este documento explica **o que mudou** em relação ao código original e **por quê**,
com foco nos dois pedidos do refactor: desacoplamento máximo e schema de banco fora
da responsabilidade do Hibernate.

## 1. Estrutura de pacotes (por feature, um único pacote raiz)

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

Isso substitui a duplicidade original (`com.achadosedevolvidos.controller` vs
`com.example.api.controller`, ambos mapeando `/api/v1/items`) por um único
`ItemController`.

## 2. Quem cria o banco: Flyway, não o Hibernate

`spring.jpa.hibernate.ddl-auto` está em **`validate`** — o Hibernate só confere, na
subida da aplicação, se as entidades batem com o schema já existente. Ele nunca
cria, altera ou apaga uma tabela.

Quem efetivamente cria e versiona o schema é o **Flyway**
(`src/main/resources/db/migration/V1` a `V7`):

| Migration | Conteúdo |
|---|---|
| V1 | `users` |
| V2 | `categories` |
| V3 | `items` |
| V4 | `item_images` |
| V5 | `matches` |
| V6 | `messages` |
| V7 | seed de categorias iniciais (DML separado do DDL) |

Vantagens diretas: histórico de mudanças de schema versionado e revisável em PR,
mesmo comportamento em dev/homologação/produção, e nenhuma surpresa de o Hibernate
"adivinhar" uma migração de coluna incorretamente.

## 3. Desacoplamento entre os módulos `item` e `match`

Esta foi a mudança mais importante para o pedido de desacoplar ao máximo.

**Antes (implícito no protótipo):** o Controller/Service de Item chamaria o motor
de match diretamente, e o motor de match operaria em cima da própria entidade JPA
`Item`.

**Agora:**

1. `ItemServiceImpl` não importa nada do pacote `match`. Ao salvar um item, ele só
   publica um `ItemCreatedEvent` via `ApplicationEventPublisher` — um mecanismo do
   Spring, não um contrato do módulo match.
2. `match.listener.ItemCreatedEventListener` escuta esse evento com
   `@TransactionalEventListener(phase = AFTER_COMMIT)` — só roda depois que a
   criação do item foi de fato persistida — e `@Async`, numa thread separada
   (`AsyncConfig`), para que uma lentidão ou falha do motor de match nunca afete a
   resposta HTTP de quem criou o item. Erros são capturados e logados, nunca
   propagados.
3. Dentro do módulo match, o **motor de pontuação em si** (`MatchEngineService`)
   foi reescrito para operar sobre `MatchCandidate` — um `record` simples, sem
   nenhuma dependência de JPA — em vez da entidade `Item`. Isso significa que ele
   pode ser instanciado com um `new MatchEngineService()` e testado sem subir
   contexto Spring nem tocar em banco (ver `MatchEngineServiceTest`). Quem sabe de
   JPA é a camada acima, `MatchServiceImpl`.

Resultado: `item` e `match` só se conhecem por um evento e por um `record` puro —
dá pra evoluir ou até substituir o motor de match sem tocar no módulo de itens.

## 4. Autenticação e autorização

Mantido o que foi entregue anteriormente (Bearer JWT + OAuth2 Google isolados
entre si — detalhes em `README-AUTH.md`), com dois acréscimos deste refactor:

- **`ItemController.create()` corrigido**: lia `@RequestAttribute("userId")`, que
  nenhum filtro preenchia. Agora usa `@AuthenticationPrincipal AuthenticatedUser`,
  que funciona tanto para quem logou via JWT quanto via Google.
- **WebSocket do chat, antes sem nenhuma autenticação**: `StompAuthChannelInterceptor`
  agora exige e valida um Bearer JWT no frame STOMP `CONNECT`, reaproveitando o
  mesmo `JwtService` do mecanismo REST (sem duplicar lógica de validação). O
  remetente da mensagem passa a vir do `Principal` autenticado da sessão STOMP,
  nunca de um campo enviado pelo próprio cliente. `ChatServiceImpl` também
  verifica que o remetente é de fato um dos dois donos de item do match antes de
  aceitar a mensagem ou liberar o histórico.

## 5. Camadas e contratos (Controller → Service (interface) → Repository)

Todo Service de escrita tem uma interface (`ItemService`, `MatchService`,
`ChatService`) implementada por uma classe `*Impl` — os Controllers dependem só da
interface, então trocar a implementação (ou mockar em teste) não exige tocar em
mais nada. Exceção proposital: `CategoryController` fala direto com o repositório,
porque é uma listagem simples sem nenhuma regra de negócio — criar uma interface
de serviço ali seria cerimônia sem benefício.

## 6. Desacoplamento Controller ↔ query dinâmica

A busca de itens (`GET /api/v1/items/search`) usa `ItemSpecifications`
(Spring Data JPA Specification) em vez de um método de repositório com uma
combinatória de parâmetros opcionais. Cada filtro é uma `Specification` isolada e
combinável — o Controller e o Service não sabem como o filtro vira SQL.

> Busca por proximidade geográfica (lat/lng) ainda não filtra no banco — precisaria
> de PostGIS ou de uma expressão Haversine em SQL nativo. Preferi deixar isso como
> próximo passo documentado a entregar uma implementação aproximada/incorreta.

## 7. Entidades sem duplicação (`BaseEntity`)

`id` (UUID) e `createdAt` deixaram de ser redeclarados em cada entidade. Uma
`@MappedSuperclass` (`BaseEntity`) centraliza os dois campos, e `createdAt` é
preenchido sozinho via `@PrePersist` — nenhum Service precisa mais lembrar de
chamar `.createdAt(LocalDateTime.now())` na mão (o `AuthService` e o
`CustomOAuth2UserService`, por exemplo, não fazem mais isso).

## 8. `open-in-view: false` exige `@Transactional` explícito em toda leitura com relação lazy

`spring.jpa.open-in-view` está desligado de propósito (ver seção 2) — a sessão
do Hibernate não fica aberta pela duração inteira da requisição HTTP por
padrão. Isso só é seguro se **todo** método de Service que navega uma relação
`@ManyToOne`/`@OneToMany` preguiçosa estiver dentro de uma transação — inclusive
métodos de leitura, não só os de escrita. Faltou isso em quatro lugares
(`ItemServiceImpl.findById`/`search`, `MatchServiceImpl.findMatchesForItem`,
`ChatServiceImpl.history`), descoberto só ao escrever os testes de integração
com Postgres real (`mvn verify` — ver `README.md`, seção 2.4): com mocks, o
`Item`/`Match` "lazy" retornado nunca é um proxy de verdade, então o teste
unitário não pega esse tipo de erro. Os quatro agora têm
`@Transactional(readOnly = true)`.

## 9. Testes automatizados

Cobertura completa (unitária + integração) descrita em `README.md`, seção 2.4.
Resumo: testes unitários (Mockito, sem Spring/banco) para os Services com
lógica de negócio; testes de integração (`@SpringBootTest` + Postgres real via
Testcontainers) cobrindo os endpoints REST, o fluxo assíncrono
item→evento→match, a conexão WebSocket/STOMP autenticada, e o callback do
login Google simulado via WireMock. Pipeline de CI em
`.github/workflows/backend-ci.yml`.

## 10. O que ainda fica para depois (fora do escopo deste refactor)

- Rate limiting no `/api/v1/auth/login`.
- Busca geográfica por raio (PostGIS).
- Paginação em `/api/v1/items/search` (hoje retorna a lista inteira).
- Publicação do app OAuth2 no Google Cloud Console (hoje em modo "Testing").
