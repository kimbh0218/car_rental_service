package org.example.dto;

import org.example.domain.ParkingLot;

import java.util.List;

public class ParkingLotAvailability {
    private final ParkingLot parkingLot;
    private final int availableCarCount;
    private final List<String> availableCarNames;

    public ParkingLotAvailability(
            ParkingLot parkingLot,
            int availableCarCount,
            List<String> availableCarNames
    ) {
        this.parkingLot = parkingLot;
        this.availableCarCount = availableCarCount;
        this.availableCarNames = availableCarNames;
    }

    public ParkingLot getParkingLot() {
        return parkingLot;
    }

    public int getAvailableCarCount() {
        return availableCarCount;
    }

    public boolean isRentalAvailable() {
        return availableCarCount > 0;
    }

    public List<String> getAvailableCarNames() {
        return availableCarNames;
    }
}
