package org.example.domain;

import java.time.LocalDateTime;

public class Rental {
    private final Long id;
    private final LocalDateTime startTime;
    private final LocalDateTime returnTime;
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;
    private final int passengersCount;
    private final RentalStatus rentalStatus;

    private Long carId;
    private Long memberId;
    private Long pickupParkingLotId;


    //새로 만들 때 필요한 생성자
    public Rental (LocalDateTime startTime, LocalDateTime returnTime, int passengersCount, Long carId, Long memberId, Long pickupParkingLotId) {
        this.id = null;
        this.startTime = startTime;
        this.returnTime = returnTime;
        this.actualPickupTime = null;
        this.actualReturnTime = null;
        this.passengersCount = passengersCount;
        this.rentalStatus = RentalStatus.RESERVED;
        this.carId = carId;
        this.memberId = memberId;
        this.pickupParkingLotId = pickupParkingLotId;
    }

    // DB 조회시 필요한 생성자
    public Rental (
            Long id, LocalDateTime startTime, LocalDateTime returnTime,
            int passengersCount, LocalDateTime actualPickupTime, LocalDateTime actualReturnTime, RentalStatus rentalStatus
            , Long carId, Long memberId, Long pickupParkingLotId
        ) {
        this.id = id;
        this.startTime = startTime;
        this.returnTime = returnTime;
        this.passengersCount = passengersCount;
        this.actualPickupTime = actualPickupTime;
        this.actualReturnTime = actualReturnTime;
        this.rentalStatus = rentalStatus;
        this.carId = carId;
        this.memberId = memberId;
        this.pickupParkingLotId = pickupParkingLotId;
    }

    @Override
    public String toString() {
        return "Rental{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", returnTime=" + returnTime +
                ", actualPickupTime=" + actualPickupTime +
                ", actualReturnTime=" + actualReturnTime +
                ", passengersCount=" + passengersCount +
                ", rentalStatus=" + rentalStatus +
                ", carId=" + carId +
                ", memberId=" + memberId +
                ", pickupParkingLotId=" + pickupParkingLotId +
                '}';
    }

        public Long getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getReturnTime() {
        return returnTime;
    }

    public LocalDateTime getActualPickupTime() {
        return actualPickupTime;
    }

    public LocalDateTime getActualReturnTime() {
        return actualReturnTime;
    }

    public int getPassengersCount() {
        return passengersCount;
    }

    public RentalStatus getRentalStatus() {
        return rentalStatus;
    }

    public Long getCarId() {
        return carId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPickupParkingLotId() {
        return pickupParkingLotId;
    }

}
