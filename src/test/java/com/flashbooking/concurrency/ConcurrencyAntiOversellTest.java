package com.flashbooking.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.flashbooking.command.domain.EventAggregate;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.SoldOutException;
import com.flashbooking.command.handler.ReserveTicketsCommandHandler;
import com.flashbooking.command.model.ReserveTicketsCommand;
import com.flashbooking.infrastructure.lock.DistributedLock;
import com.flashbooking.infrastructure.messaging.DomainEventPublisher;
import com.flashbooking.infrastructure.persistence.EventRepository;
import com.flashbooking.infrastructure.persistence.ReservationRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConcurrencyAntiOversellTest {

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

  @Test
  @DisplayName("Simulate 200 concurrent users attempting to reserve 50 available seats: exactly 50 succeed, 150 rejected")
  void simulate200UsersCompetingFor50Seats() throws InterruptedException {
    int totalSeats = 50;
    int totalUsers = 200;
    UUID eventId = UUID.randomUUID();
    EventAggregate event = new EventAggregate(eventId, "Flash Sale Festival", totalSeats);

    when(distributedLock.acquire(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    when(reservationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
    when(eventRepository.findByIdWithLock(eventId)).thenReturn(Optional.of(event));
    when(reservationRepository.save(any(ReservationAggregate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch startGun = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(totalUsers);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger soldOutCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    for (int i = 1; i <= totalUsers; i++) {
      final int userIndex = i;
      executor.submit(() -> {
        try {
          startGun.await(); // wait until all threads are ready to fire simultaneously
          ReserveTicketsCommand command = new ReserveTicketsCommand(
              eventId,
              "user-" + userIndex + "@test.com",
              1,
              "idempotency-" + userIndex + "-" + UUID.randomUUID()
          );
          synchronized (event) { // simulate DB pessimistic lock serialize point
            handler.handle(command);
          }
          successCount.incrementAndGet();
        } catch (SoldOutException e) {
          soldOutCount.incrementAndGet();
        } catch (Exception e) {
          errorCount.incrementAndGet();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startGun.countDown(); // fire all 200 threads simultaneously
    doneLatch.await();
    executor.shutdown();

    // Verify invariants:
    assertThat(successCount.get()).isEqualTo(50);
    assertThat(soldOutCount.get()).isEqualTo(150);
    assertThat(errorCount.get()).isZero();
    assertThat(event.getAvailableSeats()).isZero();
  }
}
