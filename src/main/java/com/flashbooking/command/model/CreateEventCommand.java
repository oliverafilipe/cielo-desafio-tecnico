package com.flashbooking.command.model;

public record CreateEventCommand(
    String name,
    int totalSeats
) implements Command {

  public CreateEventCommand {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Event name cannot be blank");
    }
    if (totalSeats <= 0) {
      throw new IllegalArgumentException("Total seats must be greater than zero");
    }
  }
}
