package com.flashbooking.api.controller.dto;

import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.query.view.ReservationView;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    UUID eventId,
    String customerId,
    int quantity,
    String status,
    Instant expiresAt,
    Instant createdAt
) {

  public static ReservationResponse fromAggregate(ReservationAggregate aggregate) {
    return new ReservationResponse(
        aggregate.getId(),
        aggregate.getEventId(),
        aggregate.getCustomerId(),
        aggregate.getQuantity(),
        aggregate.getStatus().name(),
        aggregate.getExpiresAt(),
        aggregate.getCreatedAt()
    );
  }

  public static ReservationResponse fromView(ReservationView view) {
    return new ReservationResponse(
        view.getReservationId(),
        view.getEventId(),
        view.getCustomerId(),
        view.getQuantity(),
        view.getStatus(),
        view.getExpiresAt(),
        view.getCreatedAt()
    );
  }
}
