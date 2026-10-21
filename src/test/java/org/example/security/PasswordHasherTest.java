package org.example.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPasswordWithCurrentPolicy() {
        String hash = passwordHasher.hash("password1234");

        assertTrue(hash.startsWith("600000:"));
        assertTrue(passwordHasher.matches("password1234", hash));
        assertFalse(passwordHasher.matches("wrong-password", hash));
        assertFalse(passwordHasher.needsRehash(hash));
    }

    @Test
    void recognizesHashCreatedWithOlderIterationPolicy() {
        assertTrue(passwordHasher.needsRehash("210000:c2FsdA==:aGFzaA=="));
    }
}
