package com.flashbooking.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventAggregate {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "total_seats", nullable = false)
  private int totalSeats;

  @Column(name = "available_seats", nullable = false)
  private int availableSeats;

  @Version
  @Column(name = "version")
  private Long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EventAggregate() {
  }

  public EventAggregate(UUID id, String name, int totalSeats) {
    if (totalSeats <= 0) {
      throw new IllegalArgumentException("Total seats must be greater than zero");
    }
    this.id = id != null ? id : UUID.randomUUID();
    this.name = name;
    this.totalSeats = totalSeats;
    this.availableSeats = totalSeats;
    this.createdAt = Instant.now();
  }

  public void reserve(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity to reserve must be greater than zero");
    }
    if (this.availableSeats < quantity) {
      throw new SoldOutException(
          String.format("Sold out: requested %d seats but only %d available for event %s",
              quantity, this.availableSeats, this.id));
    }
    this.availableSeats -= quantity;
  }

  public void releaseSeats(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity to release must be greater than zero");
    }
    this.availableSeats = Math.min(this.totalSeats, this.availableSeats + quantity);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getTotalSeats() {
    return totalSeats;
  }

  public int getAvailableSeats() {
    return availableSeats;
  }

  public Long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
