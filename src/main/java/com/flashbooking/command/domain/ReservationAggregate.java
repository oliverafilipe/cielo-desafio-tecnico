package com.flashbooking.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationAggregate {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReservationStatus status;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ReservationAggregate() {
  }

  public ReservationAggregate(
      UUID id,
      UUID eventId,
      String customerId,
      int quantity,
      String idempotencyKey,
      Instant expiresAt
  ) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    this.id = id != null ? id : UUID.randomUUID();
    this.eventId = eventId;
    this.customerId = customerId;
    this.quantity = quantity;
    this.status = ReservationStatus.PENDING;
    this.idempotencyKey = idempotencyKey;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void confirm() {
    if (this.status != ReservationStatus.PENDING) {
      throw new InvalidReservationException(
          String.format("Cannot confirm reservation %s in status %s", this.id, this.status));
    }
    if (Instant.now().isAfter(this.expiresAt)) {
      throw new InvalidReservationException(
          String.format("Reservation %s has already expired", this.id));
    }
    this.status = ReservationStatus.CONFIRMED;
    this.updatedAt = Instant.now();
  }

  public void cancel() {
    if (this.status == ReservationStatus.CANCELLED || this.status == ReservationStatus.EXPIRED) {
      throw new InvalidReservationException(
          String.format("Reservation %s is already %s", this.id, this.status));
    }
    this.status = ReservationStatus.CANCELLED;
    this.updatedAt = Instant.now();
  }

  public void expire() {
    if (this.status != ReservationStatus.PENDING) {
      throw new InvalidReservationException(
          String.format("Cannot expire reservation %s in status %s", this.id, this.status));
    }
    this.status = ReservationStatus.EXPIRED;
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public int getQuantity() {
    return quantity;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
