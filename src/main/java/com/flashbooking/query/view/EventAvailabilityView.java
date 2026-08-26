package com.flashbooking.query.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_availability_view")
public class EventAvailabilityView {

  @Id
  @Column(name = "event_id", nullable = false, updatable = false)
  private UUID eventId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "total_seats", nullable = false)
  private int totalSeats;

  @Column(name = "available_seats", nullable = false)
  private int availableSeats;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected EventAvailabilityView() {
  }

  public EventAvailabilityView(
      UUID eventId,
      String name,
      int totalSeats,
      int availableSeats,
      Instant updatedAt
  ) {
    this.eventId = eventId;
    this.name = name;
    this.totalSeats = totalSeats;
    this.availableSeats = availableSeats;
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getTotalSeats() {
    return totalSeats;
  }

  public void setTotalSeats(int totalSeats) {
    this.totalSeats = totalSeats;
  }

  public int getAvailableSeats() {
    return availableSeats;
  }

  public void setAvailableSeats(int availableSeats) {
    this.availableSeats = availableSeats;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
