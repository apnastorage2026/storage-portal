package com.apnastorage.users;

import com.apnastorage.security.Pbkdf2Sha256PasswordEncoder;
import com.apnastorage.users.model.User;
import com.apnastorage.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.password.pbkdf2-iterations=10000")
@AutoConfigureMockMvc
class SignupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private Pbkdf2Sha256PasswordEncoder passwords;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void deleteCreatedUsers() {
        users.findAll().stream()
                .filter(user -> user.getUsername().startsWith("su"))
                .forEach(users::delete);
    }

    @Test
    void signupCreatesUserAndReturnsToken() throws Exception {
        String username = "su" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String mob = "8" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        String body = """
                {
                  "name": "  Ada  ",
                  "username": "  %s  ",
                  "email": "Ada@Example.COM",
                  "mob": " %s ",
                  "password": "  password123  "
                }
                """.formatted(username, mob);

        MvcResult slash = mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        User saved = users.findByUsername(username).orElseThrow();
        assertEquals("Ada", saved.getName());
        assertEquals("Ada@example.com", saved.getEmail());
        assertEquals(mob, saved.getMob());
        assertTrue(passwords.matches("password123", saved.getPassword()));
        assertFalse(saved.getPassword().contains("password123"));
        assertTrue(saved.isActive());
        assertFalse(saved.isStaff());
        assertFalse(saved.isSuperuser());
        assertEquals("", saved.getFirstName());
        assertEquals("", saved.getLastName());
        assertNull(saved.getLastLogin());
        assertTrue(saved.getDateJoined().isAfter(Instant.now().minus(2, ChronoUnit.MINUTES)));

        String token = JsonPath.read(slash.getResponse().getContentAsString(), "$.token");
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertEquals(saved.getId().toString(), claims.getSubject());
        assertEquals(username, claims.get("username", String.class));

        String otherMob = "7" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bea","username":"%s","email":"","mob":"%s","password":"password123"}
                                """.formatted(username + "b", otherMob)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username + "b"));
    }

    @Test
    void signupNormalizesUsernameToNfkc() throws Exception {
        String username = "su\uFB01" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String stored = java.text.Normalizer.normalize(username, java.text.Normalizer.Form.NFKC);
        String mob = "6" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));

        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","username":"%s","mob":"%s","password":"password123"}
                                """.formatted(username, mob)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(stored));
    }

    @Test
    void signupReportsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","username":"bad name","email":"not-an-email","mob":"","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name[0]").value("This field may not be blank."))
                .andExpect(jsonPath("$.username[0]").value(
                        "Enter a valid username. This value may contain only letters, numbers, and @/./+/-/_ characters."))
                .andExpect(jsonPath("$.email[0]").value("Enter a valid email address."))
                .andExpect(jsonPath("$.mob[0]").value("This field may not be blank."))
                .andExpect(jsonPath("$.password[0]").value("Ensure this field has at least 8 characters."));

        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name[0]").value("This field is required."))
                .andExpect(jsonPath("$.username[0]").value("This field is required."))
                .andExpect(jsonPath("$.mob[0]").value("This field is required."))
                .andExpect(jsonPath("$.password[0]").value("This field is required."))
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","username":"%s","mob":"%s","password":"password123"}
                                """.formatted("n".repeat(101), "u".repeat(151), "m".repeat(16))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name[0]").value("Ensure this field has no more than 100 characters."))
                .andExpect(jsonPath("$.username[0]").value("Ensure this field has no more than 150 characters."))
                .andExpect(jsonPath("$.mob[0]").value("Ensure this field has no more than 15 characters."));
    }

    @Test
    void signupRejectsDuplicates() throws Exception {
        String username = "su" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String mob = "5" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        String body = """
                {"name":"Ada","username":"%s","mob":"%s","password":"password123"}
                """.formatted(username, mob);
        mockMvc.perform(post("/api/signup/").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bea","username":"%s","mob":"4%014d","password":"password123"}
                                """.formatted(username, ThreadLocalRandom.current().nextLong(10_000_000_000_000L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username[0]").value("A user with that username already exists."));

        mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bea","username":"%s","mob":"%s","password":"password123"}
                                """.formatted(username + "x", mob)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mob[0]").value("user with this mob already exists."));
    }

    @Test
    void wrongMethodIsJson() throws Exception {
        mockMvc.perform(get("/api/signup/"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.detail").value("Method \"GET\" not allowed."));
    }
}
