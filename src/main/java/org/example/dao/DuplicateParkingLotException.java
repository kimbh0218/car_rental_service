package org.example.dao;

/** DB의 주차장 이름·주소 UNIQUE 제약조건 위반을 전달합니다. */
public class DuplicateParkingLotException extends RuntimeException {
    public DuplicateParkingLotException(Throwable cause) {
        super("같은 이름과 주소의 주차장이 이미 등록되어 있습니다.", cause);
    }
}
