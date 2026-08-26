package com.flashbooking.events;

import java.time.Instant;
import java.util.UUID;

public record ReservationExpired(
    UUID reservationId,
    UUID eventId,
    int quantity,
    Instant occurredOn
) implements DomainEvent {

  public ReservationExpired(UUID reservationId, UUID eventId, int quantity) {
    this(reservationId, eventId, quantity, Instant.now());
  }
}
