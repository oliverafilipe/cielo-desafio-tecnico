package com.flashbooking.infrastructure.persistence;

import com.flashbooking.command.domain.EventAggregate;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<EventAggregate, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM EventAggregate e WHERE e.id = :id")
  Optional<EventAggregate> findByIdWithLock(@Param("id") UUID id);
}
