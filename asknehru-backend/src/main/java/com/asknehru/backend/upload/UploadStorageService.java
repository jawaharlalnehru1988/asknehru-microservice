package com.asknehru.backend.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadStorageService {

    private final String baseUrl;
    private final Path yogaImageDirectory;
    private final Path yogaAudioDirectory;

    public UploadStorageService(
            @Value("${asknehru.base-url}") String baseUrl,
            @Value("${asknehru.yoga-image-upload-dir}") String yogaImageDirectory,
            @Value("${asknehru.yoga-audio-upload-dir}") String yogaAudioDirectory
    ) {
        this.baseUrl = baseUrl;
        this.yogaImageDirectory = Path.of(yogaImageDirectory);
        this.yogaAudioDirectory = Path.of(yogaAudioDirectory);
    }

    public UploadResponse saveImage(MultipartFile file) {
        validatePresent(file, "Please select a file to upload");
        validateContentType(file, "image/", "Only image files are allowed");
        validateSize(file, 5L * 1024 * 1024, "File size must be less than 5MB");
        return save(file, yogaImageDirectory, "/media/yoga-poses/");
    }

    public UploadResponse saveAudio(MultipartFile file) {
        validatePresent(file, "Please select a file to upload");
        validateContentType(file, "audio/", "Only audio files are allowed");
        validateSize(file, 50L * 1024 * 1024, "Audio size must be less than 50MB");
        return save(file, yogaAudioDirectory, "/media/yoga-audio/");
    }

    private UploadResponse save(MultipartFile file, Path directory, String publicPrefix) {
        try {
            Files.createDirectories(directory);
            String filename = generateFilename(file);
            Path target = directory.resolve(filename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new UploadResponse(baseUrl + publicPrefix + filename, filename);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store uploaded file", ex);
        }
    }

    private void validatePresent(MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateContentType(MultipartFile file, String allowedPrefix, String message) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(allowedPrefix)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateSize(MultipartFile file, long maxBytes, String message) {
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(message);
        }
    }

    private String generateFilename(MultipartFile file) {
        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        return UUID.randomUUID() + extension;
    }
}
