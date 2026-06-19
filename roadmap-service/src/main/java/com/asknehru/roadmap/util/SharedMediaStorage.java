package com.asknehru.roadmap.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component("roadmapSharedMediaStorage")
public class SharedMediaStorage {

    private final Path sharedUploadDirectory;

    public SharedMediaStorage(@Value("${asknehru.shared-upload-dir}") String sharedUploadDirectory) {
        this.sharedUploadDirectory = Path.of(sharedUploadDirectory);
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Files.createDirectories(sharedUploadDirectory);
            String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "");
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }
            String generatedName = UUID.randomUUID() + extension;
            Path target = sharedUploadDirectory.resolve(generatedName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return generatedName;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store uploaded file", ex);
        }
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(sharedUploadDirectory.resolve(fileName));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete stored file", ex);
        }
    }

    public Resource readAsResource(String fileName) {
        try {
            Path path = sharedUploadDirectory.resolve(fileName).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return null;
            }
            return resource;
        } catch (IOException ex) {
            return null;
        }
    }
}
