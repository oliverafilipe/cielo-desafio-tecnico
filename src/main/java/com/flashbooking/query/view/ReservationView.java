package com.flashbooking.query.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation_view")
public class ReservationView {

  @Id
  @Column(name = "reservation_id", nullable = false, updatable = false)
  private UUID reservationId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ReservationView() {
  }

  public ReservationView(
      UUID reservationId,
      UUID eventId,
      String customerId,
      int quantity,
      String status,
      Instant expiresAt,
      Instant createdAt,
      Instant updatedAt
  ) {
    this.reservationId = reservationId;
    this.eventId = eventId;
    this.customerId = customerId;
    this.quantity = quantity;
    this.status = status;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
  }

  public UUID getReservationId() {
    return reservationId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
