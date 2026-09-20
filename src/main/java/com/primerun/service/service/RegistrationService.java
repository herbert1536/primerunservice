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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final RaceRepository raceRepository;
    private final RegistrationRepository registrationRepository;

    public RegistrationService(RaceRepository raceRepository, RegistrationRepository registrationRepository) {
        this.raceRepository = raceRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public RegistrationResponseDTO registerAthlete(RegistrationRequestDTO requestDTO) {
        // Step 1: Acquire Pessimistic Lock on the Race row to prevent overselling
        Race race = raceRepository.findByIdWithPessimisticLock(requestDTO.raceId())
                .orElseThrow(() -> new ResourceNotFoundException("Race not found with ID: " + requestDTO.raceId()));

        // Step 2 & 3: Validate available slots
        if (!race.hasAvailableSlots()) {
            throw new SoldOutException("No available slots remaining for race ID: " + requestDTO.raceId());
        }

        // Step 4 & 5: Decrement available slots and save updated race entity
        race.decrementAvailableSlots();
        raceRepository.save(race);

        // Step 6: Create and save registration entity
        Registration registration = new Registration(
                race,
                requestDTO.athleteName(),
                requestDTO.athleteEmail(),
                LocalDateTime.now(),
                RegistrationStatus.CONFIRMED
        );
        Registration savedRegistration = registrationRepository.save(registration);

        // Step 7: Return DTO response
        return new RegistrationResponseDTO(
                savedRegistration.getId(),
                race.getId(),
                race.getName(),
                savedRegistration.getAthleteName(),
                savedRegistration.getAthleteEmail(),
                savedRegistration.getRegistrationDate(),
                savedRegistration.getStatus()
        );
    }
}
