package org.example.dto;

import org.example.domain.RentalStatus;

import java.time.LocalDateTime;

public record CarScheduleItem(
        LocalDateTime startTime,
        LocalDateTime returnTime,
        RentalStatus rentalStatus
) {
}
