package com.apnastorage.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Objects;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MISSING = "Authentication credentials were not provided.";
    static final String INVALID = "Invalid token.";
    static final String INACTIVE = "User inactive or deleted.";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        Object failure = request.getAttribute(AuthFailure.ATTRIBUTE);
        String detail = MISSING;
        if (Objects.equals(failure, AuthFailure.INVALID)) {
            detail = INVALID;
        } else if (Objects.equals(failure, AuthFailure.INACTIVE)) {
            detail = INACTIVE;
        } else {
            response.setHeader("WWW-Authenticate", "Token");
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"detail\":\"" + detail + "\"}");
    }
}
