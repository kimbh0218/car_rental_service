package org.example.dto;

import org.example.domain.RentalStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MyRentalItem {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final long rentalId;
    private final String carName;
    private final String currentParkingLotName;
    private final String currentAddress;
    private final int passengersCount;
    private final LocalDateTime startTime;
    private final LocalDateTime returnTime;
    private final RentalStatus rentalStatus;

    public MyRentalItem(
            long rentalId,
            String carName,
            String currentParkingLotName,
            String currentAddress,
            int passengersCount,
            LocalDateTime startTime,
            LocalDateTime returnTime,
            RentalStatus rentalStatus
    ) {
        this.rentalId = rentalId;
        this.carName = carName;
        this.currentParkingLotName = currentParkingLotName;
        this.currentAddress = currentAddress;
        this.passengersCount = passengersCount;
        this.startTime = startTime;
        this.returnTime = returnTime;
        this.rentalStatus = rentalStatus;
    }

    public long getRentalId() {
        return rentalId;
    }

    public String getCarName() {
        return carName;
    }

    public String getCurrentParkingLotName() {
        return currentParkingLotName;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public int getPassengersCount() {
        return passengersCount;
    }

    public RentalStatus getRentalStatus() {
        return rentalStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public boolean isStartAvailable() {
        return !LocalDateTime.now().isBefore(startTime);
    }

    public String getRentalPeriod() {
        return DATE_TIME_FORMATTER.format(startTime)
                + " ~ "
                + DATE_TIME_FORMATTER.format(returnTime);
    }
}
