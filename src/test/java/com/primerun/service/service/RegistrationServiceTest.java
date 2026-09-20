package com.primerun.service.service;

import com.primerun.service.domain.entity.Race;
import com.primerun.service.domain.entity.Registration;
import com.primerun.service.domain.enums.RegistrationStatus;
import com.primerun.service.dto.RegistrationRequestDTO;
import com.primerun.service.dto.RegistrationResponseDTO;
import com.primerun.service.exception.ResourceNotFoundException;
import com.primerun.service.exception.SoldOutException;
import com.primerun.service.repository.RaceRepository;
import com.primerun.service.repository.RegistrationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private Race mockRace;
    private RegistrationRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockRace = new Race(1L, "Maratona do Rio", LocalDateTime.now().plusDays(30), 100, 10);
        requestDTO = new RegistrationRequestDTO(1L, "Ayrton Senna", "ayrton@example.com");
    }

    @Test
    @DisplayName("Should successfully register an athlete when slots are available")
    void shouldRegisterSuccessfullyWhenSlotsAvailable() {
        // Arrange
        when(raceRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(mockRace));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration reg = invocation.getArgument(0);
            reg.setId(10L);
            return reg;
        });

        // Act
        RegistrationResponseDTO response = registrationService.registerAthlete(requestDTO);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.raceId()).isEqualTo(1L);
        assertThat(response.athleteName()).isEqualTo("Ayrton Senna");
        assertThat(response.athleteEmail()).isEqualTo("ayrton@example.com");
        assertThat(response.status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(mockRace.getAvailableSlots()).isEqualTo(9);

        verify(raceRepository).findByIdWithPessimisticLock(1L);
        verify(raceRepository).save(mockRace);
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("Should throw SoldOutException when no slots are available and never save registration")
    void shouldThrowSoldOutExceptionWhenNoSlotsAvailable() {
        // Arrange: set available slots to 0
        mockRace.setAvailableSlots(0);
        when(raceRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(mockRace));

        // Act & Assert
        SoldOutException exception = assertThrows(SoldOutException.class, () -> {
            registrationService.registerAthlete(requestDTO);
        });

        assertThat(exception.getMessage()).contains("No available slots remaining for race ID: 1");

        // Verify registrationRepository.save was NEVER called
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when race ID does not exist")
    void shouldThrowResourceNotFoundExceptionWhenRaceNotFound() {
        // Arrange
        when(raceRepository.findByIdWithPessimisticLock(99L)).thenReturn(Optional.empty());

        RegistrationRequestDTO invalidRequest = new RegistrationRequestDTO(99L, "Rubens", "rubens@example.com");

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            registrationService.registerAthlete(invalidRequest);
        });

        assertThat(exception.getMessage()).contains("Race not found with ID: 99");
        verify(registrationRepository, never()).save(any(Registration.class));
    }
}
