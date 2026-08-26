package com.flashbooking.query.handler;

import com.flashbooking.query.model.GetEventAvailabilityQuery;
import com.flashbooking.query.model.GetReservationQuery;
import com.flashbooking.query.view.EventAvailabilityView;
import com.flashbooking.query.view.ReservationView;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

  private final GetEventAvailabilityQueryHandler availabilityQueryHandler;
  private final GetReservationQueryHandler reservationQueryHandler;

  public QueryService(
      GetEventAvailabilityQueryHandler availabilityQueryHandler,
      GetReservationQueryHandler reservationQueryHandler
  ) {
    this.availabilityQueryHandler = availabilityQueryHandler;
    this.reservationQueryHandler = reservationQueryHandler;
  }

  public EventAvailabilityView getEventAvailability(UUID eventId) {
    return availabilityQueryHandler.handle(new GetEventAvailabilityQuery(eventId));
  }

  public ReservationView getReservation(UUID reservationId) {
    return reservationQueryHandler.handle(new GetReservationQuery(reservationId));
  }
}
