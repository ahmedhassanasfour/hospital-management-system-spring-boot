package com.ahmed.hospital.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class FileValidationService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    @Value("${file.max-size}")
    private long maxFileSize;

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "File size exceeds the allowed limit"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(contentType)) {

            throw new IllegalArgumentException(
                    "Unsupported file type"
            );
        }
    }
}