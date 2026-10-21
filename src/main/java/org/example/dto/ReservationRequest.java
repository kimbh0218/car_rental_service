package org.example.dto;

import java.time.LocalDateTime;

public class ReservationRequest {

    private final Long memberId;
    private final Long carId;
    private final LocalDateTime startTime;
    private final LocalDateTime returnTime;
    private final int passengerCount;

    //생성자
    public ReservationRequest(
            Long memberId,
            Long carId,
            LocalDateTime startTime,
            LocalDateTime returnTime,
            int passengerCount
    ) {
        this.memberId = memberId;
        this.carId = carId;
        this.startTime = startTime;
        this.returnTime = returnTime;
        this.passengerCount = passengerCount;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getCarId() {
        return carId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getReturnTime() {
        return returnTime;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

}
