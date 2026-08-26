package com.flashbooking.api.controller;

import com.flashbooking.api.controller.dto.CreateEventRequest;
import com.flashbooking.api.controller.dto.CreateEventResponse;
import com.flashbooking.api.controller.dto.EventAvailabilityResponse;
import com.flashbooking.api.controller.dto.ReservationResponse;
import com.flashbooking.api.controller.dto.ReserveTicketsRequest;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.handler.CreateEventCommandHandler;
import com.flashbooking.command.handler.ReserveTicketsCommandHandler;
import com.flashbooking.command.model.CreateEventCommand;
import com.flashbooking.command.model.ReserveTicketsCommand;
import com.flashbooking.query.handler.QueryService;
import com.flashbooking.query.view.EventAvailabilityView;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

  private final CreateEventCommandHandler createEventCommandHandler;
  private final ReserveTicketsCommandHandler reserveTicketsCommandHandler;
  private final QueryService queryService;

  public EventController(
      CreateEventCommandHandler createEventCommandHandler,
      ReserveTicketsCommandHandler reserveTicketsCommandHandler,
      QueryService queryService
  ) {
    this.createEventCommandHandler = createEventCommandHandler;
    this.reserveTicketsCommandHandler = reserveTicketsCommandHandler;
    this.queryService = queryService;
  }

  @PostMapping
  public ResponseEntity<CreateEventResponse> createEvent(
      @RequestBody CreateEventRequest request
  ) {
    CreateEventCommand command = new CreateEventCommand(request.name(), request.totalSeats());
    UUID eventId = createEventCommandHandler.handle(command);
    return ResponseEntity
        .created(URI.create("/events/" + eventId))
        .body(new CreateEventResponse(eventId, "Event created successfully"));
  }

  @GetMapping("/{id}")
  public ResponseEntity<EventAvailabilityResponse> getEvent(
      @PathVariable("id") UUID eventId
  ) {
    EventAvailabilityView view = queryService.getEventAvailability(eventId);
    return ResponseEntity.ok(EventAvailabilityResponse.fromView(view));
  }

  @GetMapping("/{id}/availability")
  public ResponseEntity<EventAvailabilityResponse> getAvailability(
      @PathVariable("id") UUID eventId
  ) {
    EventAvailabilityView view = queryService.getEventAvailability(eventId);
    return ResponseEntity.ok(EventAvailabilityResponse.fromView(view));
  }

  @PostMapping("/{id}/reservations")
  public ResponseEntity<ReservationResponse> reserveTickets(
      @PathVariable("id") UUID eventId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
      @RequestBody ReserveTicketsRequest request
  ) {
    String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
        ? idempotencyKeyHeader
        : UUID.randomUUID().toString();

    ReserveTicketsCommand command = new ReserveTicketsCommand(
        eventId,
        request.customerId(),
        request.quantity(),
        idempotencyKey
    );

    ReservationAggregate reservation = reserveTicketsCommandHandler.handle(command);
    return ResponseEntity
        .created(URI.create("/reservations/" + reservation.getId()))
        .body(ReservationResponse.fromAggregate(reservation));
  }
}
