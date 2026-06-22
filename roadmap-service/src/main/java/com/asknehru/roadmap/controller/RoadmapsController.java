package com.asknehru.roadmap.controller;

import com.asknehru.roadmap.api.RoadmapDtos.CreateRoadmapRequest;
import com.asknehru.roadmap.api.RoadmapDtos.RoadmapResponse;
import com.asknehru.roadmap.api.RoadmapDtos.UpdateRoadmapRequest;
import com.asknehru.roadmap.model.Roadmap;
import com.asknehru.roadmap.service.RoadmapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapsController {

    private final RoadmapService roadmapService;
    private final ObjectMapper objectMapper;

    public RoadmapsController(RoadmapService roadmapService, ObjectMapper objectMapper) {
        this.roadmapService = roadmapService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<RoadmapResponse> listRoadmaps() {
        return roadmapService.listRoadmaps().stream().map(RoadmapResponse::fromEntity).toList();
    }

    @GetMapping("/main-topics")
    public List<String> listMainTopics() {
        return roadmapService.listRoadmaps().stream().map(Roadmap::getMainTopic).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoadmapResponse> createRoadmapJson(@Valid @RequestBody CreateRoadmapRequest request) {
        Roadmap created = roadmapService.createRoadmap(request, null);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    @PostMapping(value = "/import-syllabus/push-roadmap", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoadmapResponse> pushRoadmapJson(@Valid @RequestBody CreateRoadmapRequest request) {
        Roadmap created = roadmapService.createRoadmap(request, null);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RoadmapResponse> createRoadmapMultipart(
             @RequestPart("data") String data,
             @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        CreateRoadmapRequest request = parseData(data, CreateRoadmapRequest.class);
        Roadmap created = roadmapService.createRoadmap(request, image);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public RoadmapResponse getRoadmap(@PathVariable Long id) {
        return RoadmapResponse.fromEntity(roadmapService.getRoadmapOr404(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RoadmapResponse updateRoadmapJson(@PathVariable Long id, @Valid @RequestBody UpdateRoadmapRequest request) {
        Roadmap updated = roadmapService.updateRoadmap(id, request, null);
        return RoadmapResponse.fromEntity(updated);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoadmapResponse updateRoadmapMultipart(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UpdateRoadmapRequest request = parseData(data, UpdateRoadmapRequest.class);
        Roadmap updated = roadmapService.updateRoadmap(id, request, image);
        return RoadmapResponse.fromEntity(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(@PathVariable Long id) {
        roadmapService.deleteRoadmap(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/user-assigned", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RoadmapResponse toggleUserAssigned(
            @PathVariable Long id,
            @RequestBody com.asknehru.roadmap.api.RoadmapDtos.ToggleUserAssignedRequest request) {
        Roadmap updated = roadmapService.toggleUserAssignedRoadmap(id, request.userAssignedRoadmap());
        return RoadmapResponse.fromEntity(updated);
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = roadmapService.getImage(filename);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @GetMapping(value = "/import-syllabus/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getImportSyllabusList() {
        return ResponseEntity.ok(roadmapService.getImportSyllabusList());
    }

    @PostMapping("/import-syllabus/{id}")
    public ResponseEntity<RoadmapResponse> importSyllabus(@PathVariable Long id) {
        Roadmap created = roadmapService.importSyllabus(id);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    private <T> T parseData(String data, Class<T> targetType) {
        try {
            return objectMapper.readValue(data, targetType);
        } catch (IOException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid multipart data");
        }
    }
}
