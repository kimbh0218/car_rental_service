package org.example.domain;

public class CarWithParkingLot {

    private final Car car;
    private final String parkingLotName;
    private final CarImage primaryImage;

    public CarWithParkingLot(Car car, String parkingLotName) {
        this(car, parkingLotName, null);
    }

    public CarWithParkingLot(
            Car car,
            String parkingLotName,
            CarImage primaryImage
    ) {
        this.car = car;
        this.parkingLotName = parkingLotName;
        this.primaryImage = primaryImage;
    }

    public Car getCar() {
        return car;
    }

    public String getParkingLotName() {
        return parkingLotName;
    }

    public CarImage getPrimaryImage() {
        return primaryImage;
    }

    @Override
    public String toString() {
        String displayParkingLotName = parkingLotName == null
                ? "주차장 없음"
                : parkingLotName;

        return "CarWithParkingLotName{" +
                "car=" + car +
                ", parkingLotName='" + displayParkingLotName + '\'' +
                '}';
    }
}
