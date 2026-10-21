package org.example.dao;

/** DB의 로그인 ID UNIQUE 제약조건 위반을 도메인 오류로 전달합니다. */
public class DuplicateLoginIdException extends RuntimeException {
    public DuplicateLoginIdException(Throwable cause) {
        super("이미 사용 중인 로그인 ID입니다.", cause);
    }
}
