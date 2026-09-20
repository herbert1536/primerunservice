package com.primerun.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RaceRequestDTO(
    @NotBlank(message = "Race name is required")
    String name,

    @NotNull(message = "Event date is required")
    LocalDateTime eventDate,

    @NotNull(message = "Total slots is required")
    @Min(value = 1, message = "Total slots must be at least 1")
    Integer totalSlots
) {}
