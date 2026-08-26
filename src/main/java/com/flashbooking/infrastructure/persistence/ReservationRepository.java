package com.flashbooking.infrastructure.persistence;

import com.flashbooking.command.domain.ReservationAggregate;
import com.flashbooking.command.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationAggregate, UUID> {

  Optional<ReservationAggregate> findByIdempotencyKey(String idempotencyKey);

  List<ReservationAggregate> findByStatusAndExpiresAtBefore(
      ReservationStatus status, Instant expiresAt);
}
