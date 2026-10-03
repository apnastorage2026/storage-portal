package com.apnastorage.users.dto;

import java.time.Instant;

public record AccountResponse(String name, String username, String email, String mob, Instant date_joined) {
}
