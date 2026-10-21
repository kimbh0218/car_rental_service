package org.example.domain;

public class ParkingLot {

    private final Long id;
    private final String name;
    private final int maxParkingCount;
    private final String address;
    private final int currentParkingCount;
    private final int rentedCarCount;
    private final ParkingLotStatus status;

    //새 주차장 등록용
    public ParkingLot (String name, int maxParkingCount, String address) {
        this.id = null;
        this.name = name;
        this.maxParkingCount = maxParkingCount;
        this.address = address;
        this.currentParkingCount = 0;
        this.rentedCarCount = 0;
        this.status = ParkingLotStatus.ACTIVE;
    }

    //DB 조회용
    public ParkingLot(
            long id,
            String name,
            int maxParkingCount,
            String address,
            int currentParkingCount,
            int rentedCarCount,
            ParkingLotStatus status
    ) {
        this.id = id;
        this.name = name;
        this.maxParkingCount = maxParkingCount;
        this.address = address;
        this.currentParkingCount = currentParkingCount;
        this.rentedCarCount = rentedCarCount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxParkingCount() {
        return maxParkingCount;
    }

    public String getAddress() {
        return address;
    }

    public int getCurrentParkingCount() {
        return currentParkingCount;
    }

    public int getRentedCarCount() {
        return rentedCarCount;
    }

    public ParkingLotStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == ParkingLotStatus.ACTIVE;
    }

    public int getAvailableParkingCount() {
        return maxParkingCount - currentParkingCount - rentedCarCount;
    }

    public boolean isAvailableForReservation() {
        return getAvailableParkingCount() > 0;
    }

    public String getParkingStatus() {
        return isAvailableForReservation() ? "AVAILABLE" : "FULL";
    }

    @Override
    public String toString() {
        return "ParkingLot{" +
                "parkingLotId=" + id +
                ", name='" + name + '\'' +
                ", maxParkingCount=" + maxParkingCount +
                ", address='" + address + '\'' +
                ", currentParkingCount=" + currentParkingCount +
                ", rentedCarCount=" + rentedCarCount +
                ", status=" + status +
                '}';
    }
}
