package org.example.domain;

public class Car {
    private final Long id;
    private final String name;
    private final int maxPassengerCount;
    private final int pricePerKm;
    private final CarStatus carStatus;
    private final Long currentParkingLotId;
    private final String currentAddress;

    //새 차량 등록용 :
    public Car (String name, int maxPassengerCount, int pricePerKm) {
        this.id = null;
        this.name = name;
        this.maxPassengerCount = maxPassengerCount;
        this.pricePerKm = pricePerKm;
        this.currentParkingLotId = null;
        this.currentAddress = null;
        this.carStatus = CarStatus.AVAILABLE;
    }
    //DB 조회용
    public Car (
            long id,
            String name,
            int maxPassengerCount,
            int pricePerKm,
            Long currentParkingLotId,
            String currentAddress,
            CarStatus carStatus
    ) {
        this.id = id;
        this.name = name;
        this.maxPassengerCount = maxPassengerCount;
        this.pricePerKm = pricePerKm;
        this.currentParkingLotId = currentParkingLotId;
        this.currentAddress = currentAddress;
        this.carStatus = carStatus;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public int getMaxPassengerCount() {
        return maxPassengerCount;
    }

    public int getPricePerKm() {
        return pricePerKm;
    }

    public Long getCurrentParkingLotId() {
        return currentParkingLotId;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public CarStatus getCarStatus() {
        return carStatus;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxPassengerCount=" + maxPassengerCount +
                ", pricePerKm=" + pricePerKm +
                ", currentParkingLotId=" + currentParkingLotId +
                ", currentAddress='" + currentAddress + '\'' +
                ", carStatus=" + carStatus +
                '}';
    }
}
