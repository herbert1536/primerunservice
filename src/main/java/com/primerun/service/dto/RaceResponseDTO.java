package com.primerun.service.dto;

import java.time.LocalDateTime;

public record RaceResponseDTO(
    Long id,
    String name,
    LocalDateTime eventDate,
    Integer totalSlots,
    Integer availableSlots
) {}
