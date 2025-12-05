package com.example.dance_community.service;

import com.example.dance_community.config.FileProperties;
import com.example.dance_community.entity.Event;
import com.example.dance_community.entity.ImageHolder;
import com.example.dance_community.entity.Post;
import com.example.dance_community.enums.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileProperties fileProperties;

    public String saveImage(MultipartFile file, ImageType type) {
        if (file == null || file.isEmpty()) {
            if (type.allowDefault()) {
                return null;
            } else {
                throw new IllegalArgumentException(type.getTypeName() + " 이미지 파일이 없습니다");
            }
        }

        try {
            String filename = generateFileName(file.getOriginalFilename());

            Path uploadPath = Paths.get(
                    fileProperties.getBaseDir(),
                    type.getDirectory()
            );

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return String.format("/%s/%s/%s",
                    fileProperties.getBaseDir(),
                    type.getDirectory(),
                    filename
            );

        } catch (IOException e) {
            throw new RuntimeException(type.getTypeName() + " 이미지 저장 실패: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                Files.delete(path);
                return;
            }

            if (filePath.startsWith("/")) {
                Path relativePath = Paths.get(filePath.substring(1));
                if (Files.exists(relativePath)) {
                    Files.delete(relativePath);
                }
            }

        } catch (IOException e) {
            System.err.println("파일 삭제 실패: " + filePath + " - " + e.getMessage());
        }
    }

    private String generateFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + originalFilename;
    }

    void processImageUpdate(ImageHolder entity, List<String> newImages, List<String> keepImages) {
        if (keepImages == null) {
            if (newImages != null && !newImages.isEmpty()) {
                List<String> currentImages = new ArrayList<>(entity.getImages());
                currentImages.addAll(newImages);
                entity.updateImages(currentImages);
            }
            return;
        }

        List<String> currentImages = entity.getImages();
        List<String> finalImages = new ArrayList<>();

        if (keepImages.isEmpty()) {
            for (String imagePath : currentImages) {
                this.deleteFile(imagePath);
            }
        } else {
            finalImages.addAll(keepImages);

            List<String> imagesToDelete = currentImages.stream()
                    .filter(img -> !keepImages.contains(img))
                    .collect(Collectors.toList());

            for (String imagePath : imagesToDelete) {
                this.deleteFile(imagePath);
            }
        }

        if (newImages != null && !newImages.isEmpty()) {
            finalImages.addAll(newImages);
        }
        entity.updateImages(finalImages);
    }
}