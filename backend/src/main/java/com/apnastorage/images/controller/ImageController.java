package com.apnastorage.images.controller;

import com.apnastorage.images.dto.ImageResponse;
import com.apnastorage.images.service.ImageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ImageController {

    private final ImageService images;

    public ImageController(ImageService images) {
        this.images = images;
    }

    @PostMapping(value = {"/api/upload-image", "/api/upload-image/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> upload(
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(images.upload(currentUserId(), image, request));
    }

    @GetMapping({"/api/images", "/api/images/"})
    public List<ImageResponse> list(HttpServletRequest request) {
        return images.list(currentUserId(), request);
    }

    private static long currentUserId() {
        return (long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
