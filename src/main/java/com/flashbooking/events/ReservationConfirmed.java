package com.flashbooking.events;

import java.time.Instant;
import java.util.UUID;

public record ReservationConfirmed(
    UUID reservationId,
    UUID eventId,
    Instant occurredOn
) implements DomainEvent {

  public ReservationConfirmed(UUID reservationId, UUID eventId) {
    this(reservationId, eventId, Instant.now());
  }
}
