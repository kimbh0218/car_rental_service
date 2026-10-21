package org.example.service;

import org.example.dao.AdminDao;
import org.example.dao.AdminRepository;
import org.example.domain.Admin;
import org.example.domain.AdminAuth;
import org.example.security.PasswordHasher;

import java.util.Locale;

public class AdminService {
    private final AdminRepository adminRepository;
    private final PasswordHasher passwordHasher;

    public AdminService(AdminDao adminDao, PasswordHasher passwordHasher) {
        this((AdminRepository) adminDao, passwordHasher);
    }

    AdminService(AdminRepository adminRepository, PasswordHasher passwordHasher) {
        this.adminRepository = adminRepository;
        this.passwordHasher = passwordHasher;
    }

    public Admin login(String loginId, String rawPassword) {
        String normalizedLoginId = normalizeLoginId(loginId);
        if (!isValidLoginId(normalizedLoginId) || !passwordHasher.isValidRawPassword(rawPassword)) {
            throw invalidCredentials();
        }

        AdminAuth adminAuth = adminRepository.findAuthByLoginId(normalizedLoginId)
                .orElseThrow(this::invalidCredentials);
        if (!passwordHasher.matches(rawPassword, adminAuth.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (passwordHasher.needsRehash(adminAuth.getPasswordHash())) {
            adminRepository.updatePasswordHash(
                    adminAuth.getAdmin().getId(),
                    passwordHasher.hash(rawPassword)
            );
        }

        return adminAuth.getAdmin();
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? null : loginId.strip().toLowerCase(Locale.ROOT);
    }

    private boolean isValidLoginId(String loginId) {
        return loginId != null && loginId.matches("[a-zA-Z0-9._-]{4,50}");
    }

    private IllegalArgumentException invalidCredentials() {
        return new IllegalArgumentException("로그인 ID 또는 비밀번호가 올바르지 않습니다.");
    }
}
