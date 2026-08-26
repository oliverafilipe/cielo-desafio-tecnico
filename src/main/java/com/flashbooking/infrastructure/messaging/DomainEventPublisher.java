package com.flashbooking.infrastructure.messaging;

import com.flashbooking.events.DomainEvent;

public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
