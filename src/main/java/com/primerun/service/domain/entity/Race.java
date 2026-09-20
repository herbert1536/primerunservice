package com.primerun.service.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "races")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    public Race() {
    }

    public Race(Long id, String name, LocalDateTime eventDate, Integer totalSlots, Integer availableSlots) {
        this.id = id;
        this.name = name;
        this.eventDate = eventDate;
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
    }

    public Race(String name, LocalDateTime eventDate, Integer totalSlots) {
        this.name = name;
        this.eventDate = eventDate;
        this.totalSlots = totalSlots;
        this.availableSlots = totalSlots;
    }

    public Race(String name, LocalDateTime eventDate, Integer totalSlots, Integer availableSlots) {
        this.name = name;
        this.eventDate = eventDate;
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public boolean hasAvailableSlots() {
        return this.availableSlots != null && this.availableSlots > 0;
    }

    public void decrementAvailableSlots() {
        if (this.availableSlots <= 0) {
            throw new IllegalStateException("No available slots to decrement");
        }
        this.availableSlots--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Race race = (Race) o;
        return Objects.equals(id, race.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
