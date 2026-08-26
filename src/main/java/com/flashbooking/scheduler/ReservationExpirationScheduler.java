package com.flashbooking.scheduler;

import com.flashbooking.command.domain.EventAggregate;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.ReservationStatus;
import com.flashbooking.events.ReservationExpired;
import com.flashbooking.infrastructure.lock.DistributedLock;
import com.flashbooking.infrastructure.messaging.DomainEventPublisher;
import com.flashbooking.infrastructure.persistence.EventRepository;
import com.flashbooking.infrastructure.persistence.ReservationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationExpirationScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);
  private static final String SCHEDULER_LOCK_KEY = "lock:scheduler:reservation-expiration";
  private static final Duration LOCK_TTL = Duration.ofSeconds(10);

  private final ReservationRepository reservationRepository;
  private final EventRepository eventRepository;
  private final DistributedLock distributedLock;
  private final DomainEventPublisher eventPublisher;

  public ReservationExpirationScheduler(
      ReservationRepository reservationRepository,
      EventRepository eventRepository,
      DistributedLock distributedLock,
      DomainEventPublisher eventPublisher
  ) {
    this.reservationRepository = reservationRepository;
    this.eventRepository = eventRepository;
    this.distributedLock = distributedLock;
    this.eventPublisher = eventPublisher;
  }

  @Scheduled(fixedDelayString = "${flashbooking.scheduler.expiration-delay-ms:5000}")
  @Transactional
  public void expirePendingReservations() {
    String lockValue = UUID.randomUUID().toString();
    boolean locked = distributedLock.acquire(SCHEDULER_LOCK_KEY, lockValue, LOCK_TTL);

    if (!locked) {
      log.debug("Another instance is already running the reservation expiration job.");
      return;
    }

    try {
      Instant now = Instant.now();
      List<ReservationAggregate> expiredReservations =
          reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);

      if (!expiredReservations.isEmpty()) {
        log.info("Found {} expired reservations to process", expiredReservations.size());
      }

      for (ReservationAggregate reservation : expiredReservations) {
        reservation.expire();
        reservationRepository.save(reservation);

        // Restore seats
        eventRepository.findByIdWithLock(reservation.getEventId()).ifPresent(event -> {
          event.releaseSeats(reservation.getQuantity());
          eventRepository.save(event);
        });

        // Publish domain event
        eventPublisher.publish(new ReservationExpired(
            reservation.getId(),
            reservation.getEventId(),
            reservation.getQuantity()
        ));

        log.info("Reservation {} expired and {} seats returned to event {}",
            reservation.getId(), reservation.getQuantity(), reservation.getEventId());
      }
    } finally {
      distributedLock.release(SCHEDULER_LOCK_KEY, lockValue);
    }
  }
}
