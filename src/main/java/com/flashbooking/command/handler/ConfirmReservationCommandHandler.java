package com.flashbooking.command.handler;

import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.ResourceNotFoundException;
import com.flashbooking.command.model.ConfirmReservationCommand;
import com.flashbooking.events.ReservationConfirmed;
import com.flashbooking.infrastructure.messaging.DomainEventPublisher;
import com.flashbooking.infrastructure.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmReservationCommandHandler
    implements CommandHandler<ConfirmReservationCommand, ReservationAggregate> {

  private final ReservationRepository reservationRepository;
  private final DomainEventPublisher eventPublisher;

  public ConfirmReservationCommandHandler(
      ReservationRepository reservationRepository,
      DomainEventPublisher eventPublisher
  ) {
    this.reservationRepository = reservationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public ReservationAggregate handle(ConfirmReservationCommand command) {
    ReservationAggregate reservation = reservationRepository.findById(command.reservationId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Reservation not found: " + command.reservationId()));

    reservation.confirm();
    ReservationAggregate saved = reservationRepository.save(reservation);

    eventPublisher.publish(new ReservationConfirmed(
        reservation.getId(),
        reservation.getEventId()
    ));

    return saved;
  }
}
