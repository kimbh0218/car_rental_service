package org.example.domain;

import java.time.LocalDateTime;

public class Admin {
    private final Long id;
    private final String name;
    private final String loginId;
    private final LocalDateTime createdAt;

    public Admin(long id, String name, String loginId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.loginId = loginId;
        this.createdAt = createdAt;
    }

    public Admin(String name, String loginId) {
        this.id = null;
        this.name = name;
        this.loginId = loginId;
        this.createdAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLoginId() {
        return loginId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
