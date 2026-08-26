package com.flashbooking.command.handler;

import com.flashbooking.command.domain.EventAggregate;
import com.flashbooking.command.model.CreateEventCommand;
import com.flashbooking.events.EventCreated;
import com.flashbooking.infrastructure.messaging.DomainEventPublisher;
import com.flashbooking.infrastructure.persistence.EventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEventCommandHandler implements CommandHandler<CreateEventCommand, UUID> {

  private final EventRepository eventRepository;
  private final DomainEventPublisher eventPublisher;

  public CreateEventCommandHandler(
      EventRepository eventRepository,
      DomainEventPublisher eventPublisher
  ) {
    this.eventRepository = eventRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public UUID handle(CreateEventCommand command) {
    UUID eventId = UUID.randomUUID();
    EventAggregate event = new EventAggregate(eventId, command.name(), command.totalSeats());
    eventRepository.save(event);

    eventPublisher.publish(new EventCreated(eventId, command.name(), command.totalSeats()));
    return eventId;
  }
}
