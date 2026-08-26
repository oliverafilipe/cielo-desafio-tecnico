package com.flashbooking.infrastructure.persistence;

import com.flashbooking.query.view.ReservationView;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationViewRepository
    extends JpaRepository<ReservationView, UUID> {
}
