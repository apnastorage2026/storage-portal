package com.apnastorage.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Django's {@code pbkdf2_sha256} hash:
 * {@code pbkdf2_sha256$<iterations>$<salt>$<base64>}.
 * The salt is a random string. The hash is 32 bytes.
 */
@Component
public class Pbkdf2Sha256PasswordEncoder {

    private static final char[] SALT_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int SALT_LENGTH = 22;
    private static final int KEY_BITS = 32 * 8;

    private final SecureRandom random = new SecureRandom();

    public String encode(String rawPassword, int iterations) {
        return encode(rawPassword, randomSalt(), iterations);
    }

    public String encode(String rawPassword, String salt, int iterations) {
        String hash = Base64.getEncoder().encodeToString(pbkdf2(rawPassword, salt, iterations));
        return "pbkdf2_sha256$" + iterations + "$" + salt + "$" + hash;
    }

    public boolean matches(String rawPassword, String encoded) {
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] actual = pbkdf2(rawPassword, parts[2], iterations);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        return MessageDigest.isEqual(actual, expected);
    }

    private byte[] pbkdf2(String rawPassword, String salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(
                rawPassword.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                iterations,
                KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not hash the password.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private String randomSalt() {
        char[] salt = new char[SALT_LENGTH];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = SALT_ALPHABET[random.nextInt(SALT_ALPHABET.length)];
        }
        return new String(salt);
    }
}
