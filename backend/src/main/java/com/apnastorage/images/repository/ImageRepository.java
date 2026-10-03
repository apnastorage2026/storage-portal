package com.apnastorage.images.repository;

import com.apnastorage.images.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByUser_IdOrderByUploadedAtDescIdDesc(Long userId);
}
