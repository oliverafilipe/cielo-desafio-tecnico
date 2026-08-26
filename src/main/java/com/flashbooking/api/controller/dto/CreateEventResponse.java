package com.flashbooking.api.controller.dto;

import java.util.UUID;

public record CreateEventResponse(
    UUID eventId,
    String message
) {}
