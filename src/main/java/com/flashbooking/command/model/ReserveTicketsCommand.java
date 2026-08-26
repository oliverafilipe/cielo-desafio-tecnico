package com.flashbooking.command.model;

import java.util.UUID;

public record ReserveTicketsCommand(
    UUID eventId,
    String customerId,
    int quantity,
    String idempotencyKey
) implements Command {

  public ReserveTicketsCommand {
    if (eventId == null) {
      throw new IllegalArgumentException("Event ID cannot be null");
    }
    if (customerId == null || customerId.isBlank()) {
      throw new IllegalArgumentException("Customer ID cannot be blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("Idempotency key cannot be blank");
    }
  }
}
