package com.apnastorage.images.service;

import com.apnastorage.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Writes image bytes under {@code MEDIA_ROOT/user_<id>/<filename>}.
 * Names follow Django: trim, spaces become underscores, then drop every other character.
 */
@Component
public class ImageStorage {

    private static final Pattern ILLEGAL = Pattern.compile("[^-\\w.]", Pattern.UNICODE_CHARACTER_CLASS);
    private static final char[] ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int MAX_PATH_LENGTH = 100;
    private static final int NAME_TOKEN_LENGTH = 7;

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    private final Path root;
    private final SecureRandom random = new SecureRandom();

    public ImageStorage(AppProperties properties) {
        this.root = Path.of(properties.getMediaRoot()).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public StoredFile store(long userId, String originalFilename, byte[] bytes, String extensionIfMissing)
            throws IOException {
        String filename = filename(originalFilename, extensionIfMissing);
        String directory = "user_" + userId;
        Files.createDirectories(root.resolve(directory));
        String relative = available(directory, filename);
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Refusing to write outside the media folder.");
        }
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return new StoredFile(relative, target);
    }

    public void delete(Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(normalized);
        } catch (IOException exception) {
            log.warn("Could not delete {} after the database save failed", normalized, exception);
        }
    }

    static String sanitize(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.trim().replace(' ', '_');
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = ILLEGAL.matcher(name).replaceAll("");
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return "";
        }
        return name;
    }

    private String filename(String originalFilename, String extensionIfMissing) {
        String sanitized = sanitize(originalFilename);
        if (sanitized.isEmpty()) {
            return "image" + extensionIfMissing;
        }
        if (extension(sanitized).isEmpty()) {
            return sanitized + extensionIfMissing;
        }
        return sanitized;
    }

    private String available(String directory, String filename) throws IOException {
        String rootName = rootName(filename);
        String extension = extension(filename);
        String candidate = directory + "/" + filename;
        for (int attempt = 0; attempt < 50; attempt++) {
            if (!Files.exists(root.resolve(candidate)) && candidate.length() <= MAX_PATH_LENGTH) {
                return candidate;
            }
            int reserved = directory.length() + 1 + rootName.length() + 1 + NAME_TOKEN_LENGTH + extension.length();
            int overflow = Math.max(0, reserved - MAX_PATH_LENGTH);
            if (overflow > 0) {
                if (rootName.length() <= overflow) {
                    throw new IOException("The file name is too long.");
                }
                rootName = rootName.substring(0, rootName.length() - overflow);
            }
            candidate = directory + "/" + rootName + "_" + randomToken() + extension;
        }
        throw new IOException("Could not find a free file name.");
    }

    private static String rootName(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return filename;
        }
        return filename.substring(0, dot);
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return "";
        }
        return filename.substring(dot);
    }

    private String randomToken() {
        char[] token = new char[NAME_TOKEN_LENGTH];
        for (int i = 0; i < token.length; i++) {
            token[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(token);
    }

    public record StoredFile(String relativePath, Path path) {
    }
}
