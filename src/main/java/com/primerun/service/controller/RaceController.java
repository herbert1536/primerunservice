package com.primerun.service.controller;

import com.primerun.service.dto.RaceRequestDTO;
import com.primerun.service.dto.RaceResponseDTO;
import com.primerun.service.service.RaceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/races")
public class RaceController {

    private final RaceService raceService;

    public RaceController(RaceService raceService) {
        this.raceService = raceService;
    }

    @PostMapping
    public ResponseEntity<RaceResponseDTO> createRace(@Valid @RequestBody RaceRequestDTO requestDTO) {
        RaceResponseDTO responseDTO = raceService.createRace(requestDTO);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(responseDTO.id())
                .toUri();

        return ResponseEntity.created(location).body(responseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RaceResponseDTO> getRaceById(@PathVariable Long id) {
        RaceResponseDTO responseDTO = raceService.getRaceById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping
    public ResponseEntity<List<RaceResponseDTO>> getAllRaces() {
        List<RaceResponseDTO> races = raceService.getAllRaces();
        return ResponseEntity.ok(races);
    }
}
