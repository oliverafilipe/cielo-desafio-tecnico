package com.flashbooking.api.controller.dto;

import com.flashbooking.query.view.EventAvailabilityView;
import java.time.Instant;
import java.util.UUID;

public record EventAvailabilityResponse(
    UUID eventId,
    String name,
    int totalSeats,
    int availableSeats,
    Instant updatedAt
) {

  public static EventAvailabilityResponse fromView(EventAvailabilityView view) {
    return new EventAvailabilityResponse(
        view.getEventId(),
        view.getName(),
        view.getTotalSeats(),
        view.getAvailableSeats(),
        view.getUpdatedAt()
    );
  }
}
