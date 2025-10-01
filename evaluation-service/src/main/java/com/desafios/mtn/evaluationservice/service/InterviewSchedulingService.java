package com.desafios.mtn.evaluationservice.service;

import com.desafios.mtn.evaluationservice.service.InterviewService.AvailableSlot;
import com.desafios.mtn.evaluationservice.service.InterviewService.InterviewerSchedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio especializado en la programación y gestión de horarios de entrevistas
 */
@Service
@Slf4j
public class InterviewSchedulingService {

    /**
     * Encuentra slots disponibles en la agenda de un entrevistador
     */
    public List<AvailableSlot> findAvailableSlots(InterviewerSchedule schedule, int requiredDurationMinutes) {
        return schedule.getAvailableSlots().stream()
            .filter(slot -> slot.getDurationMinutes() >= requiredDurationMinutes)
            .map(slot -> markPreferredSlots(slot, requiredDurationMinutes))
            .toList();
    }

    /**
     * Marca slots como preferidos basado en horarios ideales
     */
    private AvailableSlot markPreferredSlots(AvailableSlot slot, int durationMinutes) {
        // Horarios preferidos: 9:00-12:00 y 14:00-17:00
        LocalTime morningPreferredStart = LocalTime.of(9, 0);
        LocalTime morningPreferredEnd = LocalTime.of(12, 0);
        LocalTime afternoonPreferredStart = LocalTime.of(14, 0);
        LocalTime afternoonPreferredEnd = LocalTime.of(17, 0);

        boolean isPreferred = 
            (slot.getStartTime().isAfter(morningPreferredStart.minusMinutes(1)) && 
             slot.getEndTime().isBefore(morningPreferredEnd.plusMinutes(1))) ||
            (slot.getStartTime().isAfter(afternoonPreferredStart.minusMinutes(1)) && 
             slot.getEndTime().isBefore(afternoonPreferredEnd.plusMinutes(1)));

        return AvailableSlot.builder()
            .startTime(slot.getStartTime())
            .endTime(slot.getEndTime())
            .durationMinutes(slot.getDurationMinutes())
            .isPreferred(isPreferred)
            .build();
    }
}