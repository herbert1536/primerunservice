package com.primerun.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> details
) {
    public ErrorResponseDTO(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null);
    }

    public ErrorResponseDTO(int status, String error, String message, String path, Map<String, String> details) {
        this(LocalDateTime.now(), status, error, message, path, details);
    }
}
