package com.asknehru.backend.upload;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadStorageService uploadStorageService;

    public UploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping("/image")
    public ResponseEntity<UploadResponse> uploadImage(@RequestParam("file") @NotNull MultipartFile file) {
        return ResponseEntity.ok(uploadStorageService.saveImage(file));
    }

    @PostMapping("/audio")
    public ResponseEntity<UploadResponse> uploadAudio(@RequestParam("file") @NotNull MultipartFile file) {
        return ResponseEntity.ok(uploadStorageService.saveAudio(file));
    }
}
