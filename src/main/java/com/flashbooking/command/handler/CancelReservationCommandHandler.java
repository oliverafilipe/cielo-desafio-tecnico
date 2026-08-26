package com.flashbooking.command.handler;

import com.flashbooking.command.domain.EventAggregate;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.ResourceNotFoundException;
import com.flashbooking.command.model.CancelReservationCommand;
import com.flashbooking.events.ReservationCancelled;
import com.flashbooking.infrastructure.messaging.DomainEventPublisher;
import com.flashbooking.infrastructure.persistence.EventRepository;
import com.flashbooking.infrastructure.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelReservationCommandHandler
    implements CommandHandler<CancelReservationCommand, Void> {

  private final ReservationRepository reservationRepository;
  private final EventRepository eventRepository;
  private final DomainEventPublisher eventPublisher;

  public CancelReservationCommandHandler(
      ReservationRepository reservationRepository,
      EventRepository eventRepository,
      DomainEventPublisher eventPublisher
  ) {
    this.reservationRepository = reservationRepository;
    this.eventRepository = eventRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public Void handle(CancelReservationCommand command) {
    ReservationAggregate reservation = reservationRepository.findById(command.reservationId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Reservation not found: " + command.reservationId()));

    reservation.cancel();
    reservationRepository.save(reservation);

    // Return seats to event aggregate
    eventRepository.findByIdWithLock(reservation.getEventId()).ifPresent(event -> {
      event.releaseSeats(reservation.getQuantity());
      eventRepository.save(event);
    });

    eventPublisher.publish(new ReservationCancelled(
        reservation.getId(),
        reservation.getEventId(),
        reservation.getQuantity()
    ));

    return null;
  }
}
