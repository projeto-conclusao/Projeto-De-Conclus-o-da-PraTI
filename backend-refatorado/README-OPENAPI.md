# Documentação OpenAPI / Swagger — gerar tipos TypeScript para o front-end

A spec OpenAPI é gerada **automaticamente em runtime** a partir do código real
(`springdoc-openapi`, ver `config/OpenApiConfig.java`) — não existe nenhum YAML
escrito à mão para manter atualizado. Toda vez que um endpoint é criado,
alterado ou removido no backend, a spec já nasce correta na próxima subida da
aplicação.

> Público em dev/local por decisão do time — nenhum dado sensível é exposto
> aqui, só metadados dos endpoints. **Revisitar antes de expor este ambiente em
> produção** (ver `SecurityConfig.java`, comentário ao lado da regra que libera
> `/v3/api-docs/**` e `/swagger-ui/**`).

## 1. Rodando a aplicação localmente

Ver `README.md` (seções 1 e 2) para o setup completo. Resumo, já com tudo
instalado e configurado:

```bash
docker compose up -d
set -a && source .env && set +a && mvn spring-boot:run
```

Com a aplicação no ar (`http://localhost:8080`), duas URLs ficam disponíveis:

| URL | O que é |
|---|---|
| `http://localhost:8080/swagger-ui/index.html` | UI interativa — navega pelos endpoints, vê o schema de cada request/response, e testa chamadas reais direto do navegador (botão "Try it out"). |
| `http://localhost:8080/v3/api-docs` | O JSON cru da spec OpenAPI 3.0 — é isso que as ferramentas de geração de código abaixo consomem. |

Para autenticar chamadas no "Try it out" (endpoints que exigem
`Authorization: Bearer`): clique no botão **Authorize** no topo da página,
cole um `accessToken` obtido via `/api/v1/auth/login` (sem o prefixo
`Bearer `, o Swagger UI já adiciona).

## 2. Gerando tipos TypeScript a partir da spec

Com a aplicação rodando localmente, dentro do projeto do **front-end**:

```bash
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.d.ts
```

Isso gera um único arquivo `.d.ts` com um tipo por schema (`AuthResponse`,
`LoginRequest`, `MessageResponse`, `ApiError`, etc.) e um tipo `paths` mapeando
cada rota ao shape exato de request/response — direto do código real do
backend, sem precisar copiar campo por campo de nenhuma tabela de
documentação.

Alternativa, se o time preferir já sair com um cliente HTTP tipado (não só os
tipos) com hooks prontos para React Query:

```bash
npx orval --input http://localhost:8080/v3/api-docs --output src/api
```

Qualquer um dos dois pode rodar contra uma URL de CI/staging no lugar de
`localhost:8080`, assim que existir um ambiente com o backend publicado — não
precisa necessariamente rodar a aplicação localmente para gerar os tipos.

## 3. O que a spec cobre — e o que ela não cobre

Cobre automaticamente: path, método HTTP, shape de request/response (a partir
dos DTOs), e restrições de validação já anotadas nos DTOs (`@NotBlank`,
`@Email`, `@Size(min = 8)` viram `required`/`minLength` no schema).

**Não** cobre sozinha (continua documentado em prosa, no `README-AUTH.md`):
regras de negócio como "o refresh token é rotacionado a cada chamada",
"forgot-password nunca revela se o e-mail existe" ou "reset-password revoga
todas as sessões". A spec descreve *o que* a API aceita/devolve; o *porquê* e
o *comportamento* continuam no README dedicado de cada módulo.

## 4. Endpoints de auth já cobertos

Os 7 endpoints documentados em `README-AUTH.md` (register, login, refresh,
logout, forgot-password, reset-password, login Google) já aparecem na spec —
confirmado rodando a aplicação localmente e conferindo `/v3/api-docs`. Não é
necessário nenhum passo extra além do que está na seção 1 acima.
