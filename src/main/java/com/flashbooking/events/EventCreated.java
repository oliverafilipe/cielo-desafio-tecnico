package com.flashbooking.events;

import java.time.Instant;
import java.util.UUID;

public record EventCreated(
    UUID eventId,
    String name,
    int totalSeats,
    Instant occurredOn
) implements DomainEvent {

  public EventCreated(UUID eventId, String name, int totalSeats) {
    this(eventId, name, totalSeats, Instant.now());
  }
}
