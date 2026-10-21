package org.example.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Java 표준 PBKDF2로 비밀번호를 해싱하고 검증합니다. */
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String rawPassword) {
        validateRawPassword(rawPassword);

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(rawPassword, salt, ITERATIONS, HASH_LENGTH_BYTES);

        Base64.Encoder encoder = Base64.getEncoder();
        return ITERATIONS + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(hash);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":", -1);
            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            if (iterations <= 0) {
                return false;
            }

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[1]);
            byte[] expectedHash = decoder.decode(parts[2]);
            byte[] actualHash = derive(rawPassword, salt, iterations, expectedHash.length);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 성공 로그인 뒤, 이전 정책으로 만든 해시를 갱신할지 판단합니다. */
    public boolean needsRehash(String storedHash) {
        try {
            String[] parts = storedHash.split(":", -1);
            return parts.length == 3 && Integer.parseInt(parts[0]) < ITERATIONS;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isValidRawPassword(String rawPassword) {
        return rawPassword != null && rawPassword.length() >= 8;
    }

    private byte[] derive(String rawPassword, byte[] salt, int iterations, int hashLengthBytes) {
        char[] passwordChars = rawPassword.toCharArray();
        PBEKeySpec keySpec = new PBEKeySpec(passwordChars, salt, iterations, hashLengthBytes * 8);

        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("비밀번호 해시 생성에 실패했습니다.", e);
        } finally {
            keySpec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }

    private void validateRawPassword(String rawPassword) {
        if (!isValidRawPassword(rawPassword)) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
    }
}
