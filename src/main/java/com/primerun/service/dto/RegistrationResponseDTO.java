package com.primerun.service.dto;

import com.primerun.service.domain.enums.RegistrationStatus;
import java.time.LocalDateTime;

public record RegistrationResponseDTO(
    Long id,
    Long raceId,
    String raceName,
    String athleteName,
    String athleteEmail,
    LocalDateTime registrationDate,
    RegistrationStatus status
) {}
