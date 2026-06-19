package com.asknehru.backend.yoga.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class YogaMediaService {

    private final Path audioDir;
    private final Path imageDir;
    private final Path videoDir;

    public YogaMediaService(
            @Value("${asknehru.yoga-audio-upload-dir}") String audioDir,
            @Value("${asknehru.yoga-image-upload-dir}") String imageDir,
            @Value("${asknehru.yoga-video-upload-dir}") String videoDir
    ) {
        this.audioDir = Path.of(audioDir);
        this.imageDir = Path.of(imageDir);
        this.videoDir = Path.of(videoDir);
    }

    public void deleteReplacedMedia(String existingRef, String updatedRef, Path directory, String publicPrefix) {
        if (existingRef == null) {
            return;
        }
        String existing = existingRef.trim();
        String updated = updatedRef == null ? "" : updatedRef.trim();
        if (existing.equals(updated)) {
            return;
        }
        deleteManagedMedia(existing, directory, publicPrefix);
    }

    public void deleteManagedMedia(String mediaRef, Path directory, String publicPrefix) {
        if (mediaRef == null || mediaRef.isBlank()) {
            return;
        }
        String candidate = mediaRef.trim();
        if (candidate.contains("/")) {
            String normalizedPrefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
            String altPrefix = normalizedPrefix.substring(1);
            if (!candidate.startsWith(normalizedPrefix) && !candidate.startsWith(altPrefix)) {
                return;
            }
            int slash = candidate.lastIndexOf('/');
            candidate = slash >= 0 ? candidate.substring(slash + 1) : candidate;
        }

        Path target = directory.resolve(candidate);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete managed media: " + candidate, ex);
        }
    }

    public Path audioDir() {
        return audioDir;
    }

    public Path imageDir() {
        return imageDir;
    }

    public Path videoDir() {
        return videoDir;
    }
}
