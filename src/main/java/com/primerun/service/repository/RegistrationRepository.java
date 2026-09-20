package com.primerun.service.repository;

import com.primerun.service.domain.entity.Registration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    long countByRaceId(Long raceId);

    List<Registration> findByRaceId(Long raceId);
}
