package com.flashbooking.events;

import java.time.Instant;
import java.util.UUID;

public record ReservationCancelled(
    UUID reservationId,
    UUID eventId,
    int quantity,
    Instant occurredOn
) implements DomainEvent {

  public ReservationCancelled(UUID reservationId, UUID eventId, int quantity) {
    this(reservationId, eventId, quantity, Instant.now());
  }
}
