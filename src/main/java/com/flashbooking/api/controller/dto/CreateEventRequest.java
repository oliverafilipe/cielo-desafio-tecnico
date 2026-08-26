package com.flashbooking.api.controller.dto;

public record CreateEventRequest(
    String name,
    int totalSeats
) {}
