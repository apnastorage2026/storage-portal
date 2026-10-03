package com.apnastorage.users.service;

import com.apnastorage.common.FieldErrorsException;
import com.apnastorage.config.AppProperties;
import com.apnastorage.security.JwtService;
import com.apnastorage.security.Pbkdf2Sha256PasswordEncoder;
import com.apnastorage.users.dto.AccountResponse;
import com.apnastorage.users.dto.AuthResponse;
import com.apnastorage.users.dto.SigninRequest;
import com.apnastorage.users.dto.SignupRequest;
import com.apnastorage.users.model.User;
import com.apnastorage.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final SignupValidator validator;
    private final UserRepository users;
    private final UserService userService;
    private final JwtService jwtService;
    private final Pbkdf2Sha256PasswordEncoder passwords;
    private final AppProperties properties;

    public AuthService(
            SignupValidator validator,
            UserRepository users,
            UserService userService,
            JwtService jwtService,
            Pbkdf2Sha256PasswordEncoder passwords,
            AppProperties properties) {
        this.validator = validator;
        this.users = users;
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwords = passwords;
        this.properties = properties;
    }

    public AccountResponse profile(long userId) {
        User user = users.findById(userId).orElseThrow();
        return new AccountResponse(
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getMob(),
                user.getDateJoined());
    }

    public AuthResponse signup(SignupRequest request) {
        request.setName(trim(request.getName()));
        request.setUsername(trim(request.getUsername()));
        request.setEmail(trim(request.getEmail()));
        request.setMob(trim(request.getMob()));
        request.setPassword(trim(request.getPassword()));

        Map<String, List<String>> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            throw new FieldErrorsException(errors);
        }

        String username = Normalizer.normalize(request.getUsername(), Normalizer.Form.NFKC);
        String email = normalizeEmail(request.getEmail());
        rejectTakenOrTooLong(username, request.getMob());

        User user = userService.create(request.getName(), username, email, request.getMob(), request.getPassword());
        return new AuthResponse(jwtService.createToken(user), user.getUsername());
    }

    public AuthResponse signin(SigninRequest request) {
        request.setUsername(trim(request.getUsername()));
        request.setPassword(trim(request.getPassword()));

        Map<String, List<String>> errors = new LinkedHashMap<>();
        requirePresent(errors, "username", request.getUsername());
        requirePresent(errors, "password", request.getPassword());
        if (!errors.isEmpty()) {
            throw new FieldErrorsException(errors);
        }

        Optional<User> found = users.findByUsername(request.getUsername());
        if (found.isEmpty()) {
            passwords.encode(request.getPassword(), properties.getPassword().getPbkdf2Iterations());
            throw invalidCredentials();
        }

        User user = found.get();
        boolean passwordMatches = passwords.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches || !user.isActive()) {
            throw invalidCredentials();
        }
        return new AuthResponse(jwtService.createToken(user), user.getUsername());
    }

    private void rejectTakenOrTooLong(String username, String mob) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (username.length() > 150) {
            errors.put("username", List.of("Ensure this field has no more than 150 characters."));
        } else if (users.existsByUsername(username)) {
            errors.put("username", List.of("A user with that username already exists."));
        }
        if (users.existsByMob(mob)) {
            errors.put("mob", List.of("user with this mob already exists."));
        }
        if (!errors.isEmpty()) {
            throw new FieldErrorsException(errors);
        }
    }

    private static void requirePresent(Map<String, List<String>> errors, String field, String value) {
        if (value == null) {
            errors.put(field, List.of("This field is required."));
        } else if (value.isEmpty()) {
            errors.put(field, List.of("This field may not be blank."));
        }
    }

    private static FieldErrorsException invalidCredentials() {
        return new FieldErrorsException(Map.of("non_field_errors", List.of("Invalid credentials.")));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return email;
        }
        return email.substring(0, at) + "@" + email.substring(at + 1).toLowerCase(Locale.ROOT);
    }
}
