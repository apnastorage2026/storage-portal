package com.apnastorage.users;

import com.apnastorage.users.model.User;
import com.apnastorage.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.password.pbkdf2-iterations=10000")
@AutoConfigureMockMvc
class SigninApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @AfterEach
    void deleteCreatedUsers() {
        users.findAll().stream()
                .filter(user -> user.getUsername().startsWith("si"))
                .forEach(users::delete);
    }

    @Test
    void signinReturnsTokenAndLeavesLastLoginNull() throws Exception {
        String username = "si" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String mob = "3" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","username":"%s","mob":"%s","password":"password123"}
                                """.formatted(username, mob)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/signin/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"  %s  ","password":"  password123  "}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        assertNull(users.findByUsername(username).orElseThrow().getLastLogin());
    }

    @Test
    void signinRejectsUnknownWrongAndInactiveTheSameWay() throws Exception {
        String username = "si" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String mob = "2" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","username":"%s","mob":"%s","password":"password123"}
                                """.formatted(username, mob)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/signin/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"wrong-password"}
                                """.formatted(username)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.non_field_errors[0]").value("Invalid credentials."));

        mockMvc.perform(post("/api/signin/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username.toUpperCase())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.non_field_errors[0]").value("Invalid credentials."));

        User user = users.findByUsername(username).orElseThrow();
        user.setActive(false);
        users.save(user);

        mockMvc.perform(post("/api/signin/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.non_field_errors[0]").value("Invalid credentials."));
    }

    @Test
    void signinReportsMissingAndBlankFields() throws Exception {
        mockMvc.perform(post("/api/signin/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username[0]").value("This field is required."))
                .andExpect(jsonPath("$.password[0]").value("This field is required."));

        mockMvc.perform(post("/api/signin/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"   ","password":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username[0]").value("This field may not be blank."))
                .andExpect(jsonPath("$.password[0]").value("This field may not be blank."));
    }
}
