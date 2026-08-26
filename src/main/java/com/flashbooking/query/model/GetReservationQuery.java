package com.flashbooking.query.model;

import java.util.UUID;

public record GetReservationQuery(
    UUID reservationId
) implements Query {

  public GetReservationQuery {
    if (reservationId == null) {
      throw new IllegalArgumentException("Reservation ID cannot be null");
    }
  }
}
