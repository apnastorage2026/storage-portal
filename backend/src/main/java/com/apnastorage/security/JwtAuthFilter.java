package com.apnastorage.security;

import com.apnastorage.users.model.User;
import com.apnastorage.users.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads {@code Authorization: Token <jwt>} and loads the user id into the security context.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Token", 0, 5)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(5).trim();
        if (token.isEmpty() || token.indexOf(' ') >= 0) {
            request.setAttribute(AuthFailure.ATTRIBUTE, AuthFailure.INVALID);
            chain.doFilter(request, response);
            return;
        }

        try {
            long userId = jwtService.readUserId(token);
            User user = users.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                request.setAttribute(AuthFailure.ATTRIBUTE, AuthFailure.INACTIVE);
            } else {
                var authentication = new UsernamePasswordAuthenticationToken(user.getId(), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            request.setAttribute(AuthFailure.ATTRIBUTE, AuthFailure.INVALID);
        }
        chain.doFilter(request, response);
    }
}
