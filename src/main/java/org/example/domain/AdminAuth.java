package org.example.domain;

/** 로그인 검증에만 사용하는 관리자 정보입니다. */
public class AdminAuth {
    private final Admin admin;
    private final String passwordHash;

    public AdminAuth(Admin admin, String passwordHash) {
        this.admin = admin;
        this.passwordHash = passwordHash;
    }

    public Admin getAdmin() {
        return admin;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
