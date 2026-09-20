package com.primerun.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.primerun.service.domain.enums.RegistrationStatus;
import com.primerun.service.dto.RegistrationRequestDTO;
import com.primerun.service.dto.RegistrationResponseDTO;
import com.primerun.service.exception.SoldOutException;
import com.primerun.service.service.RegistrationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @Test
    @DisplayName("Should return 400 Bad Request when payload is invalid (missing email or null raceId)")
    void shouldReturn400BadRequestWhenPayloadIsInvalid() throws Exception {
        RegistrationRequestDTO invalidDto = new RegistrationRequestDTO(null, "", "invalid-email");

        mockMvc.perform(post("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.raceId").exists())
                .andExpect(jsonPath("$.details.athleteName").exists())
                .andExpect(jsonPath("$.details.athleteEmail").exists());
    }

    @Test
    @DisplayName("Should return 201 Created and Location header on successful registration")
    void shouldReturn201CreatedOnSuccess() throws Exception {
        RegistrationRequestDTO validDto = new RegistrationRequestDTO(1L, "Ayrton Senna", "ayrton@example.com");
        RegistrationResponseDTO responseDto = new RegistrationResponseDTO(
                100L, 1L, "Maratona de SP", "Ayrton Senna", "ayrton@example.com", LocalDateTime.now(), RegistrationStatus.CONFIRMED
        );

        when(registrationService.registerAthlete(any(RegistrationRequestDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/registrations/100"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.athleteName").value("Ayrton Senna"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should return 409 Conflict when race is sold out")
    void shouldReturn409ConflictWhenSoldOut() throws Exception {
        RegistrationRequestDTO requestDto = new RegistrationRequestDTO(1L, "Ayrton Senna", "ayrton@example.com");

        when(registrationService.registerAthlete(any(RegistrationRequestDTO.class)))
                .thenThrow(new SoldOutException("No available slots remaining for race ID: 1"));

        mockMvc.perform(post("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No available slots remaining for race ID: 1"));
    }
}
