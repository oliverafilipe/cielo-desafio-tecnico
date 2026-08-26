package com.flashbooking.infrastructure.messaging;

import com.flashbooking.events.DomainEvent;
import com.flashbooking.events.EventCreated;
import com.flashbooking.events.EventProjector;
import com.flashbooking.events.ReservationCancelled;
import com.flashbooking.events.ReservationConfirmed;
import com.flashbooking.events.ReservationExpired;
import com.flashbooking.events.TicketsReserved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaDomainEventPublisher implements DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);
  public static final String TOPIC_EVENTS = "flashbooking-events";

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final EventProjector eventProjector;

  public KafkaDomainEventPublisher(
      KafkaTemplate<String, Object> kafkaTemplate,
      EventProjector eventProjector
  ) {
    this.kafkaTemplate = kafkaTemplate;
    this.eventProjector = eventProjector;
  }

  @Override
  public void publish(DomainEvent event) {
    String key = event.eventId().toString();
    try {
      if (kafkaTemplate != null) {
        kafkaTemplate.send(TOPIC_EVENTS, key, event);
      }
    } catch (Exception e) {
      log.warn("Kafka not reachable or error while publishing event: {}. Falling back to direct projection.", e.getMessage());
    }

    // Direct synchronous fallback or local projection
    projectDirectly(event);
  }

  private void projectDirectly(DomainEvent event) {
    if (event instanceof EventCreated created) {
      eventProjector.on(created);
    } else if (event instanceof TicketsReserved reserved) {
      eventProjector.on(reserved);
    } else if (event instanceof ReservationConfirmed confirmed) {
      eventProjector.on(confirmed);
    } else if (event instanceof ReservationCancelled cancelled) {
      eventProjector.on(cancelled);
    } else if (event instanceof ReservationExpired expired) {
      eventProjector.on(expired);
    }
  }
}
