package org.example.service;

import org.example.dao.AdminRepository;
import org.example.domain.Admin;
import org.example.domain.AdminAuth;
import org.example.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminServiceTest {

    @Test
    void loginReturnsAuthenticatedAdmin() {
        PasswordHasher passwordHasher = new PasswordHasher();
        FakeAdminRepository repository = new FakeAdminRepository();
        repository.addExisting(
                new Admin(1, "관리자", "operator", LocalDateTime.now()),
                passwordHasher.hash("password1234")
        );
        AdminService service = new AdminService(repository, passwordHasher);

        Admin admin = service.login(" OPERATOR ", "password1234");

        assertEquals(1L, admin.getId());
        assertEquals("operator", admin.getLoginId());
    }

    @Test
    void loginRejectsWrongPassword() {
        PasswordHasher passwordHasher = new PasswordHasher();
        FakeAdminRepository repository = new FakeAdminRepository();
        repository.addExisting(
                new Admin(1, "관리자", "viewer", LocalDateTime.now()),
                passwordHasher.hash("password1234")
        );
        AdminService service = new AdminService(repository, passwordHasher);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.login("viewer", "wrong-password")
        );
    }

    private static final class FakeAdminRepository implements AdminRepository {
        private final Map<Long, Admin> admins = new HashMap<>();
        private final Map<Long, String> passwordHashes = new HashMap<>();

        void addExisting(Admin admin, String passwordHash) {
            admins.put(admin.getId(), admin);
            passwordHashes.put(admin.getId(), passwordHash);
        }

        @Override
        public Optional<Admin> findById(long id) {
            return Optional.ofNullable(admins.get(id));
        }

        @Override
        public Optional<AdminAuth> findAuthByLoginId(String loginId) {
            return admins.values().stream()
                    .filter(admin -> admin.getLoginId().equals(loginId.toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .map(admin -> new AdminAuth(admin, passwordHashes.get(admin.getId())));
        }

        @Override
        public void updatePasswordHash(long adminId, String passwordHash) {
            passwordHashes.put(adminId, passwordHash);
        }
    }
}
