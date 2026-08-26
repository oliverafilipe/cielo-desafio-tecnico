package com.flashbooking.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flashbooking.command.domain.EventAggregate;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.ReservationStatus;
import com.flashbooking.command.domain.SoldOutException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveTicketsCommandHandlerTest {

  @Mock
  private EventRepository eventRepository;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private DistributedLock distributedLock;

  @Mock
  private DomainEventPublisher eventPublisher;

  @InjectMocks
  private ReserveTicketsCommandHandler handler;

  private UUID eventId;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
  }

  @Test
  @DisplayName("Should successfully reserve tickets when capacity is available")
  void shouldReserveTicketsSuccessfully() {
    EventAggregate event = new EventAggregate(eventId, "Rock Concert", 100);
    when(reservationRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.empty());
    when(distributedLock.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(eventRepository.findByIdWithLock(eventId)).thenReturn(Optional.of(event));
    when(reservationRepository.save(any(ReservationAggregate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ReserveTicketsCommand command =
        new ReserveTicketsCommand(eventId, "user-1", 2, "key-123");

    ReservationAggregate result = handler.handle(command);

    assertThat(result).isNotNull();
    assertThat(result.getQuantity()).isEqualTo(2);
    assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
    assertThat(event.getAvailableSeats()).isEqualTo(98);

    verify(eventRepository).save(event);
    verify(reservationRepository).save(any(ReservationAggregate.class));
    verify(eventPublisher).publish(any(TicketsReserved.class));
    verify(distributedLock).release(eq("lock:event:" + eventId), anyString());
  }

  @Test
  @DisplayName("Should be idempotent and return existing reservation without decrementing seats")
  void shouldReturnExistingReservationWhenIdempotencyKeyMatches() {
    ReservationAggregate existing = new ReservationAggregate(
        UUID.randomUUID(),
        eventId,
        "user-1",
        2,
        "key-123",
        Instant.now().plus(Duration.ofMinutes(10))
    );
    when(reservationRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

    ReserveTicketsCommand command =
        new ReserveTicketsCommand(eventId, "user-1", 2, "key-123");

    ReservationAggregate result = handler.handle(command);

    assertThat(result).isSameAs(existing);
    verify(eventRepository, never()).findByIdWithLock(any());
    verify(eventRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  @DisplayName("Should throw SoldOutException when requested quantity exceeds available seats")
  void shouldThrowSoldOutExceptionWhenNotEnoughSeats() {
    EventAggregate event = new EventAggregate(eventId, "Exclusive Show", 1);
    when(reservationRepository.findByIdempotencyKey("key-456")).thenReturn(Optional.empty());
    when(distributedLock.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(eventRepository.findByIdWithLock(eventId)).thenReturn(Optional.of(event));

    ReserveTicketsCommand command =
        new ReserveTicketsCommand(eventId, "user-2", 5, "key-456");

    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(SoldOutException.class)
        .hasMessageContaining("Sold out");

    assertThat(event.getAvailableSeats()).isEqualTo(1);
    verify(reservationRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
    verify(distributedLock).release(eq("lock:event:" + eventId), anyString());
  }
}
