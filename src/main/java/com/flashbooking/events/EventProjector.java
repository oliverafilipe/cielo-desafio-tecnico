package com.flashbooking.events;

import com.flashbooking.infrastructure.persistence.EventAvailabilityViewRepository;
import com.flashbooking.infrastructure.persistence.ReservationViewRepository;
import com.flashbooking.query.view.EventAvailabilityView;
import com.flashbooking.query.view.ReservationView;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventProjector {

  private final EventAvailabilityViewRepository eventAvailabilityViewRepository;
  private final ReservationViewRepository reservationViewRepository;

  public EventProjector(
      EventAvailabilityViewRepository eventAvailabilityViewRepository,
      ReservationViewRepository reservationViewRepository
  ) {
    this.eventAvailabilityViewRepository = eventAvailabilityViewRepository;
    this.reservationViewRepository = reservationViewRepository;
  }

  @Transactional
  public void on(EventCreated event) {
    EventAvailabilityView view = new EventAvailabilityView(
        event.eventId(),
        event.name(),
        event.totalSeats(),
        event.totalSeats(),
        event.occurredOn()
    );
    eventAvailabilityViewRepository.save(view);
  }

  @Transactional
  public void on(TicketsReserved event) {
    // Update event availability view
    eventAvailabilityViewRepository.findById(event.eventId()).ifPresent(view -> {
      view.setAvailableSeats(Math.max(0, view.getAvailableSeats() - event.quantity()));
      view.setUpdatedAt(event.occurredOn());
      eventAvailabilityViewRepository.save(view);
    });

    // Create reservation view
    ReservationView resView = new ReservationView(
        event.reservationId(),
        event.eventId(),
        event.customerId(),
        event.quantity(),
        "PENDING",
        event.expiresAt(),
        event.occurredOn(),
        event.occurredOn()
    );
    reservationViewRepository.save(resView);
  }

  @Transactional
  public void on(ReservationConfirmed event) {
    reservationViewRepository.findById(event.reservationId()).ifPresent(res -> {
      res.setStatus("CONFIRMED");
      res.setUpdatedAt(event.occurredOn());
      reservationViewRepository.save(res);
    });
  }

  @Transactional
  public void on(ReservationCancelled event) {
    reservationViewRepository.findById(event.reservationId()).ifPresent(res -> {
      res.setStatus("CANCELLED");
      res.setUpdatedAt(event.occurredOn());
      reservationViewRepository.save(res);
    });

    eventAvailabilityViewRepository.findById(event.eventId()).ifPresent(view -> {
      view.setAvailableSeats(Math.min(view.getTotalSeats(), view.getAvailableSeats() + event.quantity()));
      view.setUpdatedAt(event.occurredOn());
      eventAvailabilityViewRepository.save(view);
    });
  }

  @Transactional
  public void on(ReservationExpired event) {
    reservationViewRepository.findById(event.reservationId()).ifPresent(res -> {
      res.setStatus("EXPIRED");
      res.setUpdatedAt(event.occurredOn());
      reservationViewRepository.save(res);
    });

    eventAvailabilityViewRepository.findById(event.eventId()).ifPresent(view -> {
      view.setAvailableSeats(Math.min(view.getTotalSeats(), view.getAvailableSeats() + event.quantity()));
      view.setUpdatedAt(event.occurredOn());
      eventAvailabilityViewRepository.save(view);
    });
  }
}
