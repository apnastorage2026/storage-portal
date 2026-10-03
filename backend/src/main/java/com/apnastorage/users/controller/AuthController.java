package com.apnastorage.users.controller;

import com.apnastorage.users.dto.AccountResponse;
import com.apnastorage.users.dto.AuthResponse;
import com.apnastorage.users.dto.SigninRequest;
import com.apnastorage.users.dto.SignupRequest;
import com.apnastorage.users.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/api/signup", "/api/signup/"})
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping({"/api/signin", "/api/signin/"})
    public AuthResponse signin(@RequestBody SigninRequest request) {
        return authService.signin(request);
    }

    @GetMapping({"/api/account", "/api/account/"})
    public AccountResponse account() {
        long userId = (long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authService.profile(userId);
    }
}
