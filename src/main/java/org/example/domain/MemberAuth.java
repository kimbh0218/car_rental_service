package org.example.domain;

/**
 * 로그인 검증에만 사용하는 회원 정보입니다.
 * passwordHash는 화면이나 일반 회원 조회 결과로 전달하지 않습니다.
 */
public class MemberAuth {
    private final Member member;
    private final String passwordHash;

    public MemberAuth(Member member, String passwordHash) {
        this.member = member;
        this.passwordHash = passwordHash;
    }

    public Member getMember() {
        return member;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
