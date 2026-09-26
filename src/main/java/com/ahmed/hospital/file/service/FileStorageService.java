package com.ahmed.hospital.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        String originalName =
                StringUtils.cleanPath(
                        file.getOriginalFilename() == null
                                ? "file"
                                : file.getOriginalFilename()
                );

        String extension = "";

        int lastDot = originalName.lastIndexOf('.');

        if (lastDot > 0) {
            extension = originalName.substring(lastDot);
        }

        String storedName =
                UUID.randomUUID() + extension;

        try {

            Path uploadPath =
                    Paths.get(uploadDir)
                            .toAbsolutePath()
                            .normalize();

            Files.createDirectories(uploadPath);

            Path targetLocation =
                    uploadPath.resolve(storedName)
                            .normalize();

            if (!targetLocation.startsWith(uploadPath)) {
                throw new IllegalArgumentException(
                        "Invalid file path"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return storedName;

        } catch (IOException ex) {

            throw new IllegalArgumentException(
                    "Failed to store file",
                    ex
            );
        }
    }

    public Path load(String storedName) {

        try {

            Path uploadPath =
                    Paths.get(uploadDir)
                            .toAbsolutePath()
                            .normalize();

            Path filePath =
                    uploadPath.resolve(storedName)
                            .normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new IllegalArgumentException(
                        "Invalid file path"
                );
            }

            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException(
                        "File not found"
                );
            }

            return filePath;

        } catch (Exception ex) {

            throw new IllegalArgumentException(
                    "Failed to load file",
                    ex
            );
        }
    }

    public void delete(String storedName) {

        try {

            Path filePath = load(storedName);

            Files.deleteIfExists(filePath);

        } catch (IOException ex) {

            throw new IllegalArgumentException(
                    "Failed to delete file",
                    ex
            );
        }
    }
}