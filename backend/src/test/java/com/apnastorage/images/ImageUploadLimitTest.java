package com.apnastorage.images;

import com.apnastorage.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.password.pbkdf2-iterations=10000",
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=1KB"
})
class ImageUploadLimitTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    private final RestTemplate client = new RestTemplate();

    @AfterEach
    void deleteRows() {
        users.findAll().stream()
                .filter(user -> user.getUsername().startsWith("img"))
                .forEach(users::delete);
    }

    @Test
    void oversizedUploadIsJson() {
        String token = signup();
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(new byte[2048]) {
            @Override
            public String getFilename() {
                return "big.png";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.AUTHORIZATION, "Token " + token);

        try {
            client.postForEntity(
                    "http://127.0.0.1:" + port + "/api/upload-image/",
                    new HttpEntity<>(body, headers),
                    String.class);
        } catch (HttpClientErrorException exception) {
            assertEquals(413, exception.getStatusCode().value());
            assertTrue(exception.getResponseBodyAsString().contains("\"detail\":\"Uploaded file is too large.\""));
            return;
        }
        throw new AssertionError("Expected the upload to be rejected.");
    }

    private String signup() {
        String username = "img" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
        String mob = "1" + String.format("%014d", ThreadLocalRandom.current().nextLong(10_000_000_000_000L));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = """
                {"name":"Ada","username":"%s","mob":"%s","password":"password123"}
                """.formatted(username, mob);
        ResponseEntity<String> response = client.postForEntity(
                "http://127.0.0.1:" + port + "/api/signup/",
                new HttpEntity<>(json, headers),
                String.class);
        assertEquals(201, response.getStatusCode().value());
        return JsonPath.read(response.getBody(), "$.token");
    }
}
