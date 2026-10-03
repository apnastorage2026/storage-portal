package com.apnastorage.images;

import com.apnastorage.images.repository.ImageRepository;
import com.apnastorage.users.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.password.pbkdf2-iterations=10000")
@AutoConfigureMockMvc
class ImageApiTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private static Path mediaRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ImageRepository images;

    @DynamicPropertySource
    static void mediaDirectory(DynamicPropertyRegistry registry) throws Exception {
        mediaRoot = Files.createTempDirectory("apnastorage-media");
        registry.add("app.media-root", mediaRoot::toString);
    }

    @AfterEach
    void deleteRows() {
        users.findAll().stream()
                .filter(user -> user.getUsername().startsWith("img"))
                .forEach(user -> {
                    images.deleteAll(images.findByUser_IdOrderByUploadedAtDescIdDesc(user.getId()));
                    users.delete(user);
                });
    }

    @AfterAll
    static void deleteMedia() throws Exception {
        if (mediaRoot != null && Files.exists(mediaRoot)) {
            try (var paths = Files.walk(mediaRoot)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Temp files are removed on the next pass if a handle is still open.
                    }
                });
            }
        }
    }

    @Test
    void uploadAndListOnlyTheCaller() throws Exception {
        String first = signup();
        String second = signup();

        MvcResult uploaded = mockMvc.perform(multipart("/api/upload-image/")
                        .file(new MockMultipartFile("image", "my photo.png", "image/png", PNG))
                        .header("Authorization", "Token " + first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.image").value(matchesPattern("http://localhost/media/user_\\d+/my_photo\\.png")))
                .andExpect(jsonPath("$.uploaded_at").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z")))
                .andReturn();

        String imageUrl = com.jayway.jsonpath.JsonPath.read(uploaded.getResponse().getContentAsString(), "$.image");
        String mediaPath = imageUrl.substring(imageUrl.indexOf("/media/"));
        mockMvc.perform(get(mediaPath))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));

        mockMvc.perform(multipart("/api/upload-image")
                        .file(new MockMultipartFile("image", "my photo.png", "image/png", PNG))
                        .header("Authorization", "Token " + first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.image").value(matchesPattern("http://localhost/media/user_\\d+/my_photo_[A-Za-z0-9]{7}\\.png")));

        mockMvc.perform(get("/api/images/").header("Authorization", "Token " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].image").value(matchesPattern(".*my_photo_[A-Za-z0-9]{7}\\.png")))
                .andExpect(jsonPath("$[1].image").value(matchesPattern(".*my_photo\\.png")));

        mockMvc.perform(get("/api/images").header("Authorization", "Token " + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsMissingBadAndUnauthenticatedUploads() throws Exception {
        String token = signup();

        mockMvc.perform(multipart("/api/upload-image/").header("Authorization", "Token " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.image[0]").value("No file was submitted."));

        mockMvc.perform(multipart("/api/upload-image/")
                        .file(new MockMultipartFile("image", "notes.txt", "text/plain", "hello".getBytes()))
                        .header("Authorization", "Token " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.image[0]").value(
                        "Upload a valid image. The file you uploaded was either not an image or a corrupted image."));

        mockMvc.perform(get("/api/images/"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Token"))
                .andExpect(jsonPath("$.detail").value("Authentication credentials were not provided."));

        mockMvc.perform(get("/api/images/").header("Authorization", "Token not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid token."));

        var user = users.findAll().stream().filter(row -> row.getUsername().startsWith("img")).findFirst().orElseThrow();
        user.setActive(false);
        users.save(user);
        mockMvc.perform(get("/api/images/").header("Authorization", "Token " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("User inactive or deleted."));
    }

    private String signup() throws Exception {
        String username = "img" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String mob = "9" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        MvcResult result = mockMvc.perform(post("/api/signup/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","username":"%s","mob":"%s","password":"password123"}
                                """.formatted(username, mob)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}
