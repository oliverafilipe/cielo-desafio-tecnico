package com.flashbooking.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationSchedulerTest {

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private EventRepository eventRepository;

  @Mock
  private DistributedLock distributedLock;

  @Mock
  private DomainEventPublisher eventPublisher;

  @InjectMocks
  private ReservationExpirationScheduler scheduler;

  @Test
  @DisplayName("Should expire overdue reservations and return seats to event")
  void shouldExpireReservationsAndRestoreSeats() {
    UUID eventId = UUID.randomUUID();
    UUID resId = UUID.randomUUID();
    EventAggregate event = new EventAggregate(eventId, "Festival", 50);
    // simulate 5 seats reserved previously
    event.reserve(5);

    ReservationAggregate expiredRes = new ReservationAggregate(
        resId,
        eventId,
        "user-expired",
        5,
        "key-exp-1",
        Instant.now().minusSeconds(60)
    );

    when(distributedLock.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.PENDING), any(Instant.class)))
        .thenReturn(List.of(expiredRes));
    when(eventRepository.findByIdWithLock(eventId)).thenReturn(Optional.of(event));

    scheduler.expirePendingReservations();

    assertThat(expiredRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    assertThat(event.getAvailableSeats()).isEqualTo(50);

    verify(reservationRepository).save(expiredRes);
    verify(eventRepository).save(event);
    verify(eventPublisher).publish(any(ReservationExpired.class));
  }

  @Test
  @DisplayName("Should skip expiration if lock is already held by another instance")
  void shouldSkipIfLockNotAcquired() {
    when(distributedLock.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(false);

    scheduler.expirePendingReservations();

    verify(reservationRepository, never()).findByStatusAndExpiresAtBefore(any(), any());
  }
}
