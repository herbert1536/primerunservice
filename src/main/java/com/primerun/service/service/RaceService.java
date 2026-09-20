package com.primerun.service.service;

import com.primerun.service.domain.entity.Race;
import com.primerun.service.dto.RaceRequestDTO;
import com.primerun.service.dto.RaceResponseDTO;
import com.primerun.service.exception.ResourceNotFoundException;
import com.primerun.service.repository.RaceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaceService {

    private final RaceRepository raceRepository;

    public RaceService(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    @Transactional
    public RaceResponseDTO createRace(RaceRequestDTO dto) {
        Race race = new Race(dto.name(), dto.eventDate(), dto.totalSlots());
        Race saved = raceRepository.save(race);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public RaceResponseDTO getRaceById(Long id) {
        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found with ID: " + id));
        return mapToDTO(race);
    }

    @Transactional(readOnly = true)
    public List<RaceResponseDTO> getAllRaces() {
        return raceRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    private RaceResponseDTO mapToDTO(Race race) {
        return new RaceResponseDTO(
                race.getId(),
                race.getName(),
                race.getEventDate(),
                race.getTotalSlots(),
                race.getAvailableSlots()
        );
    }
}
