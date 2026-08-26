# GEMINI.md

Arquivo de contexto do Gemini CLI para este repositório. Carregado automaticamente antes de cada
interação — evita repetir instruções a cada prompt. Rode `/memory refresh` sempre que este arquivo
for alterado.

## Project

Sistema de reserva de ingressos em modelo **flash sale** (capacidade limitada, alta concorrência).
Desafio técnico de Engenheiro de Backend Sênior/Especialista. Entrega inclui code review e
apresentação das decisões arquiteturais — o código deve tornar as decisões visíveis, não só passar
nos testes.

Documento de arquitetura de referência (fonte de verdade do desenho CQRS): `README.md`.
Este GEMINI.md trata de **como implementar** esse desenho no dia a dia.

## Tech Stack

- **Linguagem**: Java (17+)
- **Framework**: Spring Boot (Web, Data JPA, Validation)
- **Build**: Maven (`./mvnw`) — trocar para Gradle aqui se o projeto migrar
- **Banco de escrita**: PostgreSQL
- **Banco/projeção de leitura**: PostgreSQL (schema/tabelas de read model)
- **Lock distribuído / contador atômico**: Redis
- **Mensageria**: Kafka ou RabbitMQ (escolher um só antes de gerar código de mensageria)
- **Scheduler**: Spring `@Scheduled` + lock distribuído (ex. ShedLock)
- **Testes**: JUnit 5 + Mockito + AssertJ + Testcontainers (Postgres, Redis, broker)
- **Empacotamento**: Docker Compose (API, Postgres, Redis, broker)

## Architecture

CQRS com separação entre command side (consistência forte, anti-oversell) e query side
(consistência eventual). Ver diagramas completos em `README.md`.

```
src/main/java/com/flashbooking
├── api/controller          # REST Controllers — sem regra de negócio
├── command/model            # Commands (imutáveis)
├── command/handler          # Command Handlers — 1 caso de uso por handler
├── command/domain           # Aggregates: EventAggregate, ReservationAggregate
├── query/model               # Queries
├── query/handler             # Query Handlers / QueryService
├── query/view                # Read Models: EventAvailabilityView, ReservationView
├── events                    # Domain Events + Projector (consumers do broker)
├── infrastructure/persistence
├── infrastructure/lock       # Redis lock/contador atômico
├── infrastructure/messaging  # Producers/Consumers
└── scheduler                 # Job de expiração
```

## Business Rules (Non-Negotiable)

1. **Nunca permitir oversell.** Decremento de assentos sempre atômico (Redis lock e/ou
   `SELECT ... FOR UPDATE`/constraint no Postgres).
2. **Idempotência.** `POST /events/:id/reservations` respeita o header `Idempotency-Key`; repetir a
   chamada com a mesma chave não cria reserva nova nem decrementa assentos de novo.
3. **Expiração automática.** Reservas `PENDING` vencidas expiram via scheduler e devolvem os
   assentos.
4. **Consistência eventual só na leitura.** O write side é sempre forte; o read model pode atrasar
   após um evento — isso é esperado.
5. **Múltiplas instâncias.** Nenhum estado em memória local (contadores, locks, filas). Coordenação
   sempre via Redis, banco ou broker.
6. **Erros explícitos.** Sem capacidade → `409`. Não encontrado → `404`. Payload inválido → `400`.

## Code Conventions

- Seguir o **Google Java Style Guide** (indentação de 2 espaços, chaves K&R, imports sem wildcard,
  ordenação de imports, limite de linha de 100 colunas).
- Commands e Events imutáveis — preferir `record` (Java 17+) a classes com setters.
- Um handler por caso de uso; controllers só orquestram (Controller → Handler → Domain).
- Toda mudança de estado do domínio emite um `DomainEvent` correspondente.
- Nomear eventos no passado (`TicketsReserved`), commands no imperativo (`ReserveTicketsCommand`).
- Javadoc apenas em APIs públicas não triviais — não documentar o óbvio.

## Commands

```bash
./mvnw clean install            # build + testes
./mvnw test                     # testes unitários
./mvnw test -Dtest=*IT          # testes de integração (Testcontainers)
docker compose up --build       # sobe API + dependências
docker compose down -v          # derruba e limpa volumes
```

## Verification (rodar antes de considerar uma mudança concluída)

- `./mvnw test` passando, incluindo os testes de concorrência de anti-oversell e de idempotência.
- Nenhum novo endpoint sem tratamento explícito de erro (`400`/`404`/`409`).
- README e `README.md` atualizados se a mudança alterar um fluxo documentado.

## Do Not

- Não implementar Kafka e RabbitMQ ao mesmo tempo "para garantir" — escolher um.
- Não acessar repositórios diretamente do controller.
- Não introduzir cache ou réplica de leitura antes de o fluxo de escrita (anti-oversell +
  idempotência) estar testado.
- Não silenciar exceções — todo erro de negócio deve virar uma resposta HTTP explícita.
- Não commitar segredos (credenciais de banco, broker) — usar variáveis de ambiente no
  `docker-compose.yml`.
