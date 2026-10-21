package org.example.domain;

public class Member {
    private final Long id;
    private final String name;
    private final boolean licenseStatus;
    private final String loginId;

    public Member(long id, String name, boolean licenseStatus, String loginId) {
        this.id = id;
        this.name = name;
        this.licenseStatus = licenseStatus;
        this.loginId = loginId;
    }

    public Member(String name, boolean licenseStatus, String loginId) {
        this.id = null;
        this.name = name;
        this.licenseStatus = licenseStatus;
        this.loginId = loginId;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }
    public boolean isLicenseStatus() {
        return licenseStatus;
    }

    @Override
    public String toString() {
        return "Member{" +
                ", name='" + name + '\'' +
                ", licenseStatus=" + licenseStatus +
                '}';

    }
}
