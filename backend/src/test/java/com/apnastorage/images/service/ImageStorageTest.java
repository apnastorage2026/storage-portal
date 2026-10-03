package com.apnastorage.images.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageStorageTest {

    @Test
    void sanitizeMatchesDjangoRules() {
        assertEquals("my_photo.png", ImageStorage.sanitize("  my photo.png  "));
        assertEquals("photo.png", ImageStorage.sanitize("../../photo.png"));
        assertEquals("ab.png", ImageStorage.sanitize("a+b.png"));
        assertEquals("", ImageStorage.sanitize("."));
    }
}
