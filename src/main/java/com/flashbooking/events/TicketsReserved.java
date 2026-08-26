package com.flashbooking.events;

import java.time.Instant;
import java.util.UUID;

public record TicketsReserved(
    UUID reservationId,
    UUID eventId,
    String customerId,
    int quantity,
    String idempotencyKey,
    Instant expiresAt,
    Instant occurredOn
) implements DomainEvent {

  public TicketsReserved(
      UUID reservationId,
      UUID eventId,
      String customerId,
      int quantity,
      String idempotencyKey,
      Instant expiresAt
  ) {
    this(reservationId, eventId, customerId, quantity, idempotencyKey, expiresAt, Instant.now());
  }
}
