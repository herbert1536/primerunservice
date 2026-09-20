package com.primerun.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequestDTO(
    @NotNull(message = "Race ID is required")
    Long raceId,

    @NotBlank(message = "Athlete name is required")
    String athleteName,

    @NotBlank(message = "Athlete email is required")
    @Email(message = "Invalid email format")
    String athleteEmail
) {}
