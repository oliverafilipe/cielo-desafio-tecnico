package com.flashbooking.command.model;

import java.util.UUID;

public record ConfirmReservationCommand(
    UUID reservationId
) implements Command {

  public ConfirmReservationCommand {
    if (reservationId == null) {
      throw new IllegalArgumentException("Reservation ID cannot be null");
    }
  }
}
