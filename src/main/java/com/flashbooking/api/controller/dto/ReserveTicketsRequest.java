package com.flashbooking.api.controller.dto;

public record ReserveTicketsRequest(
    String customerId,
    int quantity
) {}
