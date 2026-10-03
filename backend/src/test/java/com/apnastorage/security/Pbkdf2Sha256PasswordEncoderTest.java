package com.apnastorage.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pbkdf2Sha256PasswordEncoderTest {

    private final Pbkdf2Sha256PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder();

    @Test
    void matchesDjangoPbkdf2Sha256() {
        assertEquals(
                "pbkdf2_sha256$1500000$saltsaltsaltsaltsalt12$io9uzMDCAqI+58P88DkcyibXXD0yxO1+vcwWSPa5+sU=",
                encoder.encode("password123", "saltsaltsaltsaltsalt12", 1_500_000));
        assertTrue(encoder.matches(
                "password123",
                "pbkdf2_sha256$1500000$saltsaltsaltsaltsalt12$io9uzMDCAqI+58P88DkcyibXXD0yxO1+vcwWSPa5+sU="));
    }
}
