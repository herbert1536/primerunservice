package com.primerun.service;

import com.primerun.service.domain.entity.Race;
import com.primerun.service.dto.RegistrationRequestDTO;
import com.primerun.service.dto.RegistrationResponseDTO;
import com.primerun.service.exception.SoldOutException;
import com.primerun.service.repository.RaceRepository;
import com.primerun.service.repository.RegistrationRepository;
import com.primerun.service.service.RegistrationService;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RaceRegistrationConcurrencyTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private Long testRaceId;

    @BeforeEach
    void setUp() {
        registrationRepository.deleteAll();
        raceRepository.deleteAll();

        // Create race with ONLY 5 available slots
        Race race = new Race("Desafio 10K High Concurrency", LocalDateTime.now().plusDays(10), 5, 5);
        Race savedRace = raceRepository.save(race);
        this.testRaceId = savedRace.getId();
    }

    @Test
    @DisplayName("High Concurrency Test: 100 threads competing for 5 slots simultaneously (Pessimistic Lock)")
    void testHighConcurrencyPessimisticLockingOversellingPrevention() throws InterruptedException {
        int numberOfConcurrentRequests = 100;
        int expectedSuccessfulRegistrations = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(numberOfConcurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfConcurrentRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            final int index = i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    RegistrationRequestDTO request = new RegistrationRequestDTO(
                            testRaceId,
                            "Athlete " + index,
                            "athlete" + index + "@primerun.com"
                    );

                    RegistrationResponseDTO response = registrationService.registerAthlete(request);
                    if (response != null && response.id() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (SoldOutException e) {
                    rejectedCount.incrementAndGet();
                } catch (DataAccessException e) {
                    rejectedCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();

        Race finalRaceState = raceRepository.findById(testRaceId).orElseThrow();
        long totalRegistrationsInDb = registrationRepository.countByRaceId(testRaceId);

        // Verification of overselling prevention rules:
        // 1. Total successful registrations in database must equal exactly the total available slots (5)
        assertThat(totalRegistrationsInDb).isEqualTo(expectedSuccessfulRegistrations);
        assertThat(successCount.get()).isEqualTo(expectedSuccessfulRegistrations);

        // 2. Remaining available slots in race must be exactly 0 (never negative)
        assertThat(finalRaceState.getAvailableSlots()).isEqualTo(0);

        // 3. Exactly 95 requests were rejected due to sold out or lock contention
        assertThat(rejectedCount.get()).isEqualTo(numberOfConcurrentRequests - expectedSuccessfulRegistrations);
        assertThat(unexpectedErrorCount.get()).isZero();
    }
}
