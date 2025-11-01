package com.training.customer_service.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CustomerAssignmentRequest(
    @NotBlank(message = "Splitter Serial Number is required.")
    String splitterSerialNumber,

    @NotNull(message = "Assigned port number is required.")
    @Min(value = 1, message = "Port number must be 1 or greater.")
    Integer portNumber,

    @NotNull(message = "Fiber length in meters is required.")
    @Min(value = 0, message = "Length cannot be negative.")
    BigDecimal lengthMeters
) {}
