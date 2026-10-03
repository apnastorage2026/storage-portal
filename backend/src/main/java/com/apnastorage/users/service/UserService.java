package com.apnastorage.users.service;

import com.apnastorage.config.AppProperties;
import com.apnastorage.security.Pbkdf2Sha256PasswordEncoder;
import com.apnastorage.users.model.User;
import com.apnastorage.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository users;
    private final Pbkdf2Sha256PasswordEncoder passwords;
    private final AppProperties properties;

    public UserService(UserRepository users, Pbkdf2Sha256PasswordEncoder passwords, AppProperties properties) {
        this.users = users;
        this.passwords = passwords;
        this.properties = properties;
    }

    @Transactional
    public User create(String name, String username, String email, String mob, String rawPassword) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setMob(mob);
        user.setPassword(passwords.encode(rawPassword, properties.getPassword().getPbkdf2Iterations()));
        user.setStaff(false);
        user.setSuperuser(false);
        user.setActive(true);
        user.setFirstName("");
        user.setLastName("");
        user.setLastLogin(null);
        user.setDateJoined(Instant.now());
        return users.save(user);
    }
}
