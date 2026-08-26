package com.flashbooking.infrastructure.persistence;

import com.flashbooking.query.view.EventAvailabilityView;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAvailabilityViewRepository
    extends JpaRepository<EventAvailabilityView, UUID> {
}
