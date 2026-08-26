package com.flashbooking.query.handler;

import com.flashbooking.command.domain.ResourceNotFoundException;
import com.flashbooking.infrastructure.persistence.ReservationViewRepository;
import com.flashbooking.query.model.GetReservationQuery;
import com.flashbooking.query.view.ReservationView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReservationQueryHandler
    implements QueryHandler<GetReservationQuery, ReservationView> {

  private final ReservationViewRepository repository;

  public GetReservationQueryHandler(ReservationViewRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public ReservationView handle(GetReservationQuery query) {
    return repository.findById(query.reservationId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Reservation not found in read model: " + query.reservationId()));
  }
}
