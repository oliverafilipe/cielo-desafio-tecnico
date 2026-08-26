package com.flashbooking.command.model;

import java.util.UUID;

public record CancelReservationCommand(
    UUID reservationId
) implements Command {

  public CancelReservationCommand {
    if (reservationId == null) {
      throw new IllegalArgumentException("Reservation ID cannot be null");
    }
  }
}
