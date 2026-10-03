package com.apnastorage.users.service;

import com.apnastorage.users.dto.SignupRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SignupValidator {

    private static final Pattern USERNAME = Pattern.compile("^[\\w.@+-]+$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");
    private static final Pattern LOCALHOST_EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@localhost$");

    public Map<String, List<String>> validate(SignupRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        requireText(errors, "name", request.getName(), 100);
        requireUsername(errors, request.getUsername());
        optionalEmail(errors, request.getEmail());
        requireText(errors, "mob", request.getMob(), 15);
        requirePassword(errors, request.getPassword());
        return errors;
    }

    private void requireText(Map<String, List<String>> errors, String field, String value, int maxLength) {
        if (value == null) {
            errors.put(field, List.of("This field is required."));
            return;
        }
        if (value.isEmpty()) {
            errors.put(field, List.of("This field may not be blank."));
            return;
        }
        if (value.length() > maxLength) {
            errors.put(field, List.of("Ensure this field has no more than " + maxLength + " characters."));
        }
    }

    private void requireUsername(Map<String, List<String>> errors, String value) {
        requireText(errors, "username", value, 150);
        if (errors.containsKey("username") || value == null) {
            return;
        }
        if (!USERNAME.matcher(value).matches()) {
            errors.put("username", List.of(
                    "Enter a valid username. This value may contain only letters, numbers, and @/./+/-/_ characters."));
        }
    }

    private void optionalEmail(Map<String, List<String>> errors, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (value.length() > 254) {
            errors.put("email", List.of("Ensure this field has no more than 254 characters."));
            return;
        }
        if (!EMAIL.matcher(value).matches() && !LOCALHOST_EMAIL.matcher(value).matches()) {
            errors.put("email", List.of("Enter a valid email address."));
        }
    }

    private void requirePassword(Map<String, List<String>> errors, String value) {
        if (value == null) {
            errors.put("password", List.of("This field is required."));
            return;
        }
        if (value.isEmpty()) {
            errors.put("password", List.of("This field may not be blank."));
            return;
        }
        if (value.length() < 8) {
            errors.put("password", List.of("Ensure this field has at least 8 characters."));
        }
    }
}
