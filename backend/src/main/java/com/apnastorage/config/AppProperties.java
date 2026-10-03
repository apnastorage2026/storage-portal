package com.apnastorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Password password = new Password();
    private String corsAllowedOrigins;
    private String mediaRoot = "./media";

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private int expirationMinutes = 1440;
    }

    @Getter
    @Setter
    public static class Password {
        private int pbkdf2Iterations = 1_500_000;
    }
}
