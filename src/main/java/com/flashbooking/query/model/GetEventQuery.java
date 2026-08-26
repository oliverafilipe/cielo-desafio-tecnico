package com.flashbooking.query.model;

import java.util.UUID;

public record GetEventQuery(
    UUID eventId
) implements Query {

  public GetEventQuery {
    if (eventId == null) {
      throw new IllegalArgumentException("Event ID cannot be null");
    }
  }
}
