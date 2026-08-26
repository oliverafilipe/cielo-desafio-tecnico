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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaDomainEventListener {

  private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventListener.class);
  private final EventProjector eventProjector;

  public KafkaDomainEventListener(EventProjector eventProjector) {
    this.eventProjector = eventProjector;
  }

  @KafkaListener(
      topics = KafkaDomainEventPublisher.TOPIC_EVENTS,
      groupId = "flashbooking-projector-group",
      autoStartup = "${spring.kafka.consumer.auto-startup:false}"
  )
  public void consume(Object message) {
    log.info("Received event message from Kafka: {}", message);
    if (message instanceof EventCreated event) {
      eventProjector.on(event);
    } else if (message instanceof TicketsReserved event) {
      eventProjector.on(event);
    } else if (message instanceof ReservationConfirmed event) {
      eventProjector.on(event);
    } else if (message instanceof ReservationCancelled event) {
      eventProjector.on(event);
    } else if (message instanceof ReservationExpired event) {
      eventProjector.on(event);
    }
  }
}
