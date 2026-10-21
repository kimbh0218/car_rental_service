package org.example.dao;

import org.example.domain.Admin;
import org.example.domain.AdminAuth;

import java.util.Optional;

public interface AdminRepository {
    Optional<Admin> findById(long id);

    Optional<AdminAuth> findAuthByLoginId(String loginId);

    void updatePasswordHash(long adminId, String passwordHash);
}
