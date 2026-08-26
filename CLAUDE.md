# CLAUDE.md

Guia de contexto para o Claude Code trabalhar neste repositório. Leia antes de gerar ou alterar código.

## Visão Geral do Projeto

Sistema de reserva de ingressos em modelo **flash sale** (capacidade limitada, alta concorrência).
Desafio técnico de Engenheiro de Backend Sênior/Especialista. Após a entrega há code review e
apresentação das decisões arquiteturais — o código precisa expressar claramente as decisões, não só
"funcionar".

Arquitetura de referência: ver `arquitetura-cqrs-kotlin.md` (diagramas de componentes, classes,
sequência e estados). Este arquivo é a fonte de verdade sobre o desenho **CQRS**; o CLAUDE.md aqui é
sobre **como implementar** esse desenho.

## Stack

- **Linguagem**: Java
- **Framework**: Spring Boot (Web, Data JPA, Validation)
- **Banco de escrita**: PostgreSQL
- **Banco/projeção de leitura**: PostgreSQL (schema/tabelas de read model) — pode evoluir para réplica
- **Lock distribuído / contador atômico**: Redis
- **Mensageria (eventos)**: Kafka ou RabbitMQ (definir um só; não implementar os dois)
- **Scheduler de expiração**: Spring `@Scheduled` + lock distribuído (ex. ShedLock) para não expirar em
  duplicidade entre instâncias
- **Testes**: JUnit 5 + Mockito + AssertJ + Testcontainers (Postgres, Redis, broker) para testes de
  integração
- **Empacotamento**: Docker Compose (API, Postgres, Redis, broker)

## Comandos

```bash
./mvnw clean install            # compila e roda testes (ajustar se o projeto usar Gradle)
./mvnw test                     # somente testes unitários
./mvnw test -Dtest=*IT           # testes de integração (Testcontainers)
docker compose up --build       # sobe API + dependências
docker compose down -v          # derruba e limpa volumes
```

Ajustar os comandos acima assim que o `pom.xml`/`build.gradle` e o `docker-compose.yml` forem
criados — não presumir que já existem.

## Estrutura de Pacotes (alvo)

```
src/main/java/com/flashbooking
├── api/controller          # REST Controllers (fino, sem regra de negócio)
├── command/model            # Commands (classes imutáveis / records)
├── command/handler          # Command Handlers (1 caso de uso por handler)
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

## Regras de Negócio Inegociáveis

Estas regras vêm direto dos requisitos não funcionais do desafio e **têm prioridade sobre
conveniência de implementação**:

1. **Nunca permitir oversell.** Toda decrementação de assentos deve ser atômica (lock distribuído no
   Redis e/ou `SELECT ... FOR UPDATE`/constraint no Postgres). Escrever teste de concorrência que
   dispare N reservas simultâneas contra capacidade < N e verificar que nenhuma excede o limite.
2. **Idempotência.** `POST /events/:id/reservations` deve aceitar um header `Idempotency-Key`. Uma
   segunda chamada com a mesma chave retorna o mesmo resultado, sem criar nova reserva nem decrementar
   assentos de novo.
3. **Expiração automática.** Reservas `PENDING` sem confirmação até `expiresAt` devem expirar
   sozinhas (scheduler) e devolver os assentos ao pool disponível.
4. **Consistência eventual só no lado de leitura.** O lado de escrita (decremento de assentos,
   criação/cancelamento de reserva) é sempre consistência forte. O read model pode ficar
   momentaneamente desatualizado após um evento — isso é aceitável e esperado, não é bug.
5. **Múltiplas instâncias simultâneas.** Nada de estado em memória de uma instância (contadores locais,
   locks em memória, filas locais). Qualquer coordenação passa por Redis, banco ou broker.
6. **Erros explícitos.** Sem capacidade → `409 Conflict`. Reserva/evento não encontrado → `404`.
   Payload inválido → `400`. Nunca engolir exceção silenciosamente.

## Convenções de Código

- Commands e Events são **imutáveis** (classes finais com campos `final`, ou `record` se o projeto usar
  Java 17+).
- Um handler por caso de uso — não criar "handlers gigantes" que tratam vários comandos.
- Controllers não acessam repositórios diretamente: sempre via Command/Query Handler.
- Toda mudança de estado do domínio deve emitir um `DomainEvent` correspondente, mesmo que o
  projector inicial seja simples.
- Nomear eventos no passado (`TicketsReserved`, `ReservationExpired`), commands no imperativo
  (`ReserveTicketsCommand`).
- Testes de concorrência/oversell e de idempotência são obrigatórios, não opcionais — não considerar
  a tarefa concluída sem eles.

## O Que Documentar no README (entrega final)

- Como subir com `docker compose up`
- Decisões arquiteturais e trade-offs (puxar de `arquitetura-cqrs-kotlin.md`, mas escrever com as
  palavras do código final, não copiar o diagrama)
- Evoluções futuras (event sourcing completo, outbox pattern, circuit breaker, cache no read model)

## Ao Gerar Código

- Sempre perguntar/checar se Kafka ou RabbitMQ já foi escolhido antes de gerar código de mensageria;
  não implementar os dois "para garantir".
- Ao adicionar um novo endpoint, atualizar tanto o diagrama de sequência quanto este arquivo se a
  regra de negócio mudar.
- Não otimizar prematuramente a leitura (cache, réplica) antes de o fluxo de escrita com
  anti-oversell e idempotência estar testado e funcionando.
