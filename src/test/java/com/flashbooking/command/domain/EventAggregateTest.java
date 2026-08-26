package com.flashbooking.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventAggregateTest {

  @Test
  @DisplayName("Should successfully initialize event and reserve seats")
  void shouldReserveSeats() {
    EventAggregate event = new EventAggregate(UUID.randomUUID(), "Concert", 10);
    assertThat(event.getAvailableSeats()).isEqualTo(10);

    event.reserve(4);
    assertThat(event.getAvailableSeats()).isEqualTo(6);

    event.releaseSeats(2);
    assertThat(event.getAvailableSeats()).isEqualTo(8);
  }

  @Test
  @DisplayName("Should prevent oversell and throw SoldOutException")
  void shouldPreventOversell() {
    EventAggregate event = new EventAggregate(UUID.randomUUID(), "Concert", 5);

    event.reserve(5);
    assertThat(event.getAvailableSeats()).isZero();

    assertThatThrownBy(() -> event.reserve(1))
        .isInstanceOf(SoldOutException.class);
  }
}
