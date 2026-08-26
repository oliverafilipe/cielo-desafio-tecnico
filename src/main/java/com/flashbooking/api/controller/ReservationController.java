package com.flashbooking.api.controller;

import com.flashbooking.api.controller.dto.ReservationResponse;
import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.handler.CancelReservationCommandHandler;
import com.flashbooking.command.handler.ConfirmReservationCommandHandler;
import com.flashbooking.command.model.CancelReservationCommand;
import com.flashbooking.command.model.ConfirmReservationCommand;
import com.flashbooking.query.handler.QueryService;
import com.flashbooking.query.view.ReservationView;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

  private final QueryService queryService;
  private final ConfirmReservationCommandHandler confirmReservationCommandHandler;
  private final CancelReservationCommandHandler cancelReservationCommandHandler;

  public ReservationController(
      QueryService queryService,
      ConfirmReservationCommandHandler confirmReservationCommandHandler,
      CancelReservationCommandHandler cancelReservationCommandHandler
  ) {
    this.queryService = queryService;
    this.confirmReservationCommandHandler = confirmReservationCommandHandler;
    this.cancelReservationCommandHandler = cancelReservationCommandHandler;
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReservationResponse> getReservation(
      @PathVariable("id") UUID reservationId
  ) {
    ReservationView view = queryService.getReservation(reservationId);
    return ResponseEntity.ok(ReservationResponse.fromView(view));
  }

  @PostMapping("/{id}/confirm")
  public ResponseEntity<ReservationResponse> confirmReservation(
      @PathVariable("id") UUID reservationId
  ) {
    ReservationAggregate confirmed = confirmReservationCommandHandler
        .handle(new ConfirmReservationCommand(reservationId));
    return ResponseEntity.ok(ReservationResponse.fromAggregate(confirmed));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancelReservation(
      @PathVariable("id") UUID reservationId
  ) {
    cancelReservationCommandHandler.handle(new CancelReservationCommand(reservationId));
    return ResponseEntity.noContent().build();
  }
}
