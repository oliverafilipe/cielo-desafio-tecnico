# Arquitetura Proposta — Flash Booking (CQRS + Kotlin + Spring Boot)

Documento de abordagem arquitetural para o desafio de reserva de ingressos, cobrindo os requisitos de
não-oversell, expiração automática, idempotência e consistência eventual, usando **CQRS** com **Kotlin**
e **Spring Boot**.

---

## 1. Visão Geral

- **Command side**: recebe escritas (criar evento, reservar, cancelar), valida invariantes de negócio e
  garante consistência forte (sem oversell) via transação/lock no banco de escrita.
- **Query side**: modelo de leitura desnormalizado, otimizado para `GET /events/:id` e `GET /reservations/:id`,
  atualizado de forma assíncrona a partir dos eventos de domínio (consistência eventual, conforme exigido).
- **Event Bus** (Kafka ou RabbitMQ): desacopla os dois lados e permite múltiplas instâncias da API sem
  estado compartilhado em memória.
- **Redis**: lock distribuído / contador atômico para reforçar não-oversell entre múltiplas instâncias.
- **Scheduler**: job periódico que expira reservas `PENDING` vencidas e devolve os assentos.

## 2. Diagrama de Componentes

```mermaid
graph TB
    Client[Cliente HTTP]

    subgraph API["API Layer (multi-instância)"]
        Ctrl[REST Controllers]
        CmdBus[Command Bus]
        QrySvc[Query Service]
    end

    subgraph Write["Command Side"]
        Handlers[Command Handlers]
        Agg[Aggregates: EventAggregate / ReservationAggregate]
        WriteDB[(Postgres - Write Model)]
        Lock[(Redis - Lock / Contador Atômico)]
    end

    Bus[[Event Bus - Kafka/RabbitMQ]]

    subgraph Read["Query Side"]
        Proj[Event Projector]
        ReadDB[(Postgres/Read Replica - Read Model)]
    end

    Sched[Scheduler - Expiração de Reservas]

    Client --> Ctrl
    Ctrl --> CmdBus --> Handlers
    Handlers --> Agg
    Handlers --> WriteDB
    Handlers --> Lock
    Handlers --> Bus
    Bus --> Proj --> ReadDB
    Ctrl --> QrySvc --> ReadDB
    Sched --> Handlers
```

## 3. Diagrama de Classes (CQRS)

```mermaid
classDiagram
    class Command
    class ReserveTicketsCommand {
      +eventId: UUID
      +quantity: Int
      +idempotencyKey: String
    }
    class CancelReservationCommand
    class CreateEventCommand

    class CommandHandler~T~ {
      <<interface>>
      +handle(command: T)
    }
    class ReserveTicketsCommandHandler
    class CancelReservationCommandHandler
    class CreateEventCommandHandler

    class EventAggregate {
      -id: UUID
      -totalSeats: Int
      -availableSeats: Int
      +reserve(qty: Int) DomainEvent
    }
    class ReservationAggregate {
      -id: UUID
      -eventId: UUID
      -status: ReservationStatus
      -expiresAt: Instant
      +confirm()
      +expire()
      +cancel()
    }

    class DomainEvent
    class TicketsReserved
    class ReservationExpired
    class ReservationCancelled
    class EventCreated

    class Query
    class GetEventAvailabilityQuery
    class GetReservationQuery
    class QueryHandler~T,R~ {
      <<interface>>
      +handle(query: T) R
    }
    class EventAvailabilityView {
      +eventId: UUID
      +availableSeats: Int
    }
    class ReservationView {
      +reservationId: UUID
      +status: String
    }
    class EventProjector {
      +on(event: TicketsReserved)
      +on(event: ReservationExpired)
    }

    Command <|-- ReserveTicketsCommand
    Command <|-- CancelReservationCommand
    Command <|-- CreateEventCommand

    CommandHandler <|.. ReserveTicketsCommandHandler
    CommandHandler <|.. CancelReservationCommandHandler
    CommandHandler <|.. CreateEventCommandHandler

    ReserveTicketsCommandHandler --> EventAggregate
    ReserveTicketsCommandHandler --> ReservationAggregate
    CancelReservationCommandHandler --> ReservationAggregate

    DomainEvent <|-- TicketsReserved
    DomainEvent <|-- ReservationExpired
    DomainEvent <|-- ReservationCancelled
    DomainEvent <|-- EventCreated

    ReservationAggregate ..> TicketsReserved : emite
    ReservationAggregate ..> ReservationExpired : emite

    Query <|-- GetEventAvailabilityQuery
    Query <|-- GetReservationQuery
    QueryHandler <|.. EventProjector

    EventProjector ..> EventAvailabilityView : projeta
    EventProjector ..> ReservationView : projeta
    TicketsReserved --> EventProjector : consumido por
    ReservationExpired --> EventProjector : consumido por
```

## 4. Sequência — Reservar Ingressos (idempotência + anti-oversell)

```mermaid
sequenceDiagram
    participant C as Cliente
    participant API as ReservationController
    participant CH as ReserveTicketsCommandHandler
    participant L as Redis (Lock/Contador)
    participant DB as WriteDB (Postgres)
    participant Bus as EventBus
    participant P as Projector
    participant RDB as ReadDB

    C->>API: POST /events/:id/reservations (header Idempotency-Key)
    API->>CH: dispatch ReserveTicketsCommand
    CH->>DB: SELECT resultado por idempotencyKey
    alt chave já processada
        DB-->>CH: reserva existente
        CH-->>API: 200/201 (resultado cacheado)
    else nova requisição
        CH->>L: acquire lock(eventId)
        CH->>DB: SELECT available_seats FOR UPDATE
        alt assentos suficientes
            CH->>DB: UPDATE seats, INSERT reservation (PENDING, idempotencyKey)
            CH->>Bus: publish TicketsReserved
            CH->>L: release lock
            Bus->>P: consome evento (async)
            P->>RDB: atualiza read model
            CH-->>API: 201 Created (reservationId, expiresAt)
        else sem assentos
            CH->>L: release lock
            CH-->>API: 409 Conflict (sold out)
        end
    end
    API-->>C: resposta HTTP
```

## 5. Sequência — Expiração Automática de Reservas

```mermaid
sequenceDiagram
    participant S as Scheduler (@Scheduled)
    participant DB as WriteDB
    participant Bus as EventBus
    participant P as Projector
    participant RDB as ReadDB

    loop a cada N segundos, por instância com lock leader/DB
        S->>DB: SELECT reservations WHERE status=PENDING AND expires_at < now()
        DB-->>S: reservas vencidas
        S->>DB: UPDATE status=EXPIRED, devolve available_seats
        S->>Bus: publish ReservationExpired (por reserva)
        Bus->>P: consome evento
        P->>RDB: atualiza disponibilidade e status da reserva
    end
```

## 6. Diagrama de Estados — Reserva

```mermaid
stateDiagram-v2
    [*] --> PENDING: criada
    PENDING --> CONFIRMED: confirmação/pagamento
    PENDING --> EXPIRED: TTL vencido (scheduler)
    PENDING --> CANCELLED: DELETE /reservations/:id
    CONFIRMED --> CANCELLED: DELETE /reservations/:id
    EXPIRED --> [*]
    CANCELLED --> [*]
    CONFIRMED --> [*]
```

## 7. Decisões Arquiteturais e Trade-offs

| Decisão | Justificativa | Trade-off |
|---|---|---|
| CQRS com bases separadas (write/read) | Isola a lógica de escrita crítica (anti-oversell) da leitura de alta concorrência | Complexidade adicional de sincronização e eventual delay na leitura |
| Lock/contador atômico no Redis + `SELECT FOR UPDATE` no Postgres | Garante decremento atômico de assentos mesmo com múltiplas instâncias | Ponto de contenção sob alta concorrência; requer TTL no lock para evitar deadlock |
| Idempotency-Key persistida na tabela de comandos/reservas | Evita reservas duplicadas em retries de rede | Exige limpeza/expiração da tabela de chaves |
| Event Bus (Kafka/RabbitMQ) para propagar eventos ao read model | Desacopla escrita e leitura, permite escalar independentemente | Consistência eventual — leitura pode ficar momentaneamente desatualizada |
| Scheduler para expiração (`@Scheduled` + lock distribuído, ex. ShedLock) | Libera assentos automaticamente sem intervenção do cliente | Necessita coordenação entre instâncias para não expirar em duplicidade |

## 8. Evoluções Futuras

- Migrar o command side para **Event Sourcing** completo (persistir eventos, não apenas estado).
- Usar **outbox pattern** para publicar eventos de forma transacional junto ao commit no write DB.
- Circuit breaker/retry (Resilience4j) na publicação de eventos.
- Escalar o read model com cache (Redis) para consultas de disponibilidade de altíssimo tráfego.

## 9. Estrutura de Pacotes (Kotlin/Spring Boot)

```
src/main/kotlin/com/flashbooking
├── api
│   └── controller        // REST Controllers
├── command
│   ├── model              // Commands
│   ├── handler            // Command Handlers
│   └── domain             // Aggregates (EventAggregate, ReservationAggregate)
├── query
│   ├── model              // Queries
│   ├── handler            // Query Handlers / QueryService
│   └── view               // Read Models (EventAvailabilityView, ReservationView)
├── events                 // Domain Events + Projector (consumers)
├── infrastructure
│   ├── persistence        // Repositories JPA (write e read)
│   ├── lock               // Redis lock/contador atômico
│   └── messaging          // Producers/Consumers Kafka/RabbitMQ
└── scheduler              // Job de expiração de reservas
```
