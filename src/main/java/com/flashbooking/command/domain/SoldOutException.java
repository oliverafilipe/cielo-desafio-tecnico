package com.flashbooking.command.domain;

public class SoldOutException extends RuntimeException {
  public SoldOutException(String message) {
    super(message);
  }
}
