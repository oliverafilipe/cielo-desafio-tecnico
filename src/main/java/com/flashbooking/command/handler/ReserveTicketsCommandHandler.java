package com.flashbooking.command.handler;

import com.flashbooking.command.domain.EventAggregate;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.ResourceNotFoundException;
import com.flashbooking.command.model.ReserveTicketsCommand;
import com.flashbooking.events.TicketsReserved;
import com.flashbooking.infrastructure.lock.DistributedLock;
import com.flashbooking.infrastructure.messaging.DomainEventPublisher;
import com.flashbooking.infrastructure.persistence.EventRepository;
import com.flashbooking.infrastructure.persistence.ReservationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveTicketsCommandHandler
    implements CommandHandler<ReserveTicketsCommand, ReservationAggregate> {

  private static final Duration DEFAULT_RESERVATION_TTL = Duration.ofMinutes(10);
  private static final Duration LOCK_TTL = Duration.ofSeconds(5);

  private final EventRepository eventRepository;
  private final ReservationRepository reservationRepository;
  private final DistributedLock distributedLock;
  private final DomainEventPublisher eventPublisher;

  public ReserveTicketsCommandHandler(
      EventRepository eventRepository,
      ReservationRepository reservationRepository,
      DistributedLock distributedLock,
      DomainEventPublisher eventPublisher
  ) {
    this.eventRepository = eventRepository;
    this.reservationRepository = reservationRepository;
    this.distributedLock = distributedLock;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public ReservationAggregate handle(ReserveTicketsCommand command) {
    // 1. Idempotency Check: if reservation already exists for this key, return it
    Optional<ReservationAggregate> existing =
        reservationRepository.findByIdempotencyKey(command.idempotencyKey());
    if (existing.isPresent()) {
      return existing.get();
    }

    String lockKey = "lock:event:" + command.eventId();
    String lockValue = UUID.randomUUID().toString();
    boolean locked = distributedLock.acquire(lockKey, lockValue, LOCK_TTL);

    try {
      // 2. Select event with pessimistic write lock (SELECT FOR UPDATE)
      EventAggregate event = eventRepository.findByIdWithLock(command.eventId())
          .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + command.eventId()));

      // 3. Atomically reserve seats
      event.reserve(command.quantity());
      eventRepository.save(event);

      // 4. Create and persist reservation in PENDING state
      UUID reservationId = UUID.randomUUID();
      Instant expiresAt = Instant.now().plus(DEFAULT_RESERVATION_TTL);
      ReservationAggregate reservation = new ReservationAggregate(
          reservationId,
          command.eventId(),
          command.customerId(),
          command.quantity(),
          command.idempotencyKey(),
          expiresAt
      );
      ReservationAggregate savedReservation = reservationRepository.save(reservation);

      // 5. Emit domain event
      eventPublisher.publish(new TicketsReserved(
          reservationId,
          command.eventId(),
          command.customerId(),
          command.quantity(),
          command.idempotencyKey(),
          expiresAt
      ));

      return savedReservation;
    } finally {
      if (locked) {
        distributedLock.release(lockKey, lockValue);
      }
    }
  }
}
