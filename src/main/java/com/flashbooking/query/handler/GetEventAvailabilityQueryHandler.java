package com.flashbooking.query.handler;

import com.flashbooking.command.domain.ResourceNotFoundException;
import com.flashbooking.infrastructure.persistence.EventAvailabilityViewRepository;
import com.flashbooking.query.model.GetEventAvailabilityQuery;
import com.flashbooking.query.view.EventAvailabilityView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetEventAvailabilityQueryHandler
    implements QueryHandler<GetEventAvailabilityQuery, EventAvailabilityView> {

  private final EventAvailabilityViewRepository repository;

  public GetEventAvailabilityQueryHandler(EventAvailabilityViewRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public EventAvailabilityView handle(GetEventAvailabilityQuery query) {
    return repository.findById(query.eventId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Event not found in read model: " + query.eventId()));
  }
}
