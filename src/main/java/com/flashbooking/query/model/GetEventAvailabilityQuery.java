package com.flashbooking.query.model;

import java.util.UUID;

public record GetEventAvailabilityQuery(
    UUID eventId
) implements Query {

  public GetEventAvailabilityQuery {
    if (eventId == null) {
      throw new IllegalArgumentException("Event ID cannot be null");
    }
  }
}
