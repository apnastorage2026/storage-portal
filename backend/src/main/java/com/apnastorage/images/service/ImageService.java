package com.apnastorage.images.service;

import com.apnastorage.common.FieldErrorsException;
import com.apnastorage.images.dto.ImageResponse;
import com.apnastorage.images.model.Image;
import com.apnastorage.images.repository.ImageRepository;
import com.apnastorage.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImageService {

    private static final Set<String> FORMATS = Set.of("jpeg", "jpg", "png", "gif", "webp");
    private static final DateTimeFormatter UPLOADED_AT = new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
    private static final String NOT_AN_IMAGE =
            "Upload a valid image. The file you uploaded was either not an image or a corrupted image.";

    private final ImageRepository images;
    private final UserRepository users;
    private final ImageStorage storage;

    public ImageService(ImageRepository images, UserRepository users, ImageStorage storage) {
        this.images = images;
        this.users = users;
        this.storage = storage;
    }

    public ImageResponse upload(long userId, MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new FieldErrorsException(Map.of("image", List.of("No file was submitted.")));
        }
        byte[] bytes = read(file);
        String format = formatOf(bytes);
        if (format == null) {
            throw new FieldErrorsException(Map.of("image", List.of(NOT_AN_IMAGE)));
        }

        ImageStorage.StoredFile stored;
        try {
            stored = storage.store(userId, file.getOriginalFilename(), bytes, extensionFor(format));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store the image.", exception);
        }

        try {
            Image image = new Image();
            image.setImage(stored.relativePath());
            image.setUploadedAt(Instant.now());
            image.setUser(users.getReferenceById(userId));
            Image saved = images.saveAndFlush(image);
            return response(saved, request);
        } catch (RuntimeException exception) {
            storage.delete(stored.path());
            throw exception;
        }
    }

    public List<ImageResponse> list(long userId, HttpServletRequest request) {
        return images.findByUser_IdOrderByUploadedAtDescIdDesc(userId).stream()
                .map(image -> response(image, request))
                .toList();
    }

    private ImageResponse response(Image image, HttpServletRequest request) {
        return new ImageResponse(image.getId(), absoluteUrl(request, image.getImage()), UPLOADED_AT.format(image.getUploadedAt()));
    }

    static String absoluteUrl(HttpServletRequest request, String relativePath) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        String host = request.getServerName() + (defaultPort ? "" : ":" + port);
        return scheme + "://" + host + "/media/" + relativePath;
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new FieldErrorsException(Map.of("image", List.of(NOT_AN_IMAGE)));
        }
    }

    private static String formatOf(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATS.contains(format)) {
                    return null;
                }
                BufferedImage decoded = reader.read(0);
                return decoded == null ? null : format;
            } finally {
                reader.dispose();
            }
        } catch (RuntimeException | IOException exception) {
            return null;
        }
    }

    private static String extensionFor(String format) {
        return switch (format) {
            case "png" -> ".png";
            case "gif" -> ".gif";
            case "webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
