package com.asknehru.roadmap.controller;

import com.asknehru.roadmap.api.RoadmapDtos.CreateRoadmapRequest;
import com.asknehru.roadmap.api.RoadmapDtos.RoadmapResponse;
import com.asknehru.roadmap.api.RoadmapDtos.UpdateRoadmapRequest;
import com.asknehru.roadmap.api.RoadmapDtos.RoadmapChapterRequest;
import com.asknehru.roadmap.api.RoadmapDtos.RoadmapSubtopicRequest;
import com.asknehru.roadmap.model.Roadmap;
import com.asknehru.roadmap.model.RoadmapChapter;
import com.asknehru.roadmap.model.RoadmapSubtopic;
import com.asknehru.roadmap.repository.RoadmapRepository;
import com.asknehru.roadmap.util.SharedMediaStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapsController {

    private final RoadmapRepository roadmapRepository;
    private final SharedMediaStorage sharedMediaStorage;
    private final ObjectMapper objectMapper;

    public RoadmapsController(
            RoadmapRepository roadmapRepository,
            @Qualifier("roadmapSharedMediaStorage") SharedMediaStorage sharedMediaStorage,
            ObjectMapper objectMapper
    ) {
        this.roadmapRepository = roadmapRepository;
        this.sharedMediaStorage = sharedMediaStorage;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<RoadmapResponse> listRoadmaps() {
        return roadmapRepository.findAllByOrderByCreatedAtDescIdDesc().stream().map(RoadmapResponse::fromEntity).toList();
    }

    @GetMapping("/main-topics")
    public List<String> listMainTopics() {
        return roadmapRepository.findAllByOrderByCreatedAtDescIdDesc().stream().map(Roadmap::getMainTopic).toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoadmapResponse> createRoadmapJson(@Valid @RequestBody CreateRoadmapRequest request) {
        Roadmap created = createRoadmap(request, null);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    @PostMapping(value = "/import-syllabus/push-roadmap", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoadmapResponse> pushRoadmapJson(@Valid @RequestBody CreateRoadmapRequest request) {
        Roadmap created = createRoadmap(request, null);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RoadmapResponse> createRoadmapMultipart(
             @RequestPart("data") String data,
             @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        CreateRoadmapRequest request = parseData(data, CreateRoadmapRequest.class);
        Roadmap created = createRoadmap(request, image);
        return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public RoadmapResponse getRoadmap(@PathVariable Long id) {
        return RoadmapResponse.fromEntity(getRoadmapOr404(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RoadmapResponse updateRoadmapJson(@PathVariable Long id, @Valid @RequestBody UpdateRoadmapRequest request) {
        Roadmap updated = updateRoadmap(id, request, null);
        return RoadmapResponse.fromEntity(updated);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoadmapResponse updateRoadmapMultipart(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UpdateRoadmapRequest request = parseData(data, UpdateRoadmapRequest.class);
        Roadmap updated = updateRoadmap(id, request, image);
        return RoadmapResponse.fromEntity(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(@PathVariable Long id) {
        Roadmap roadmap = getRoadmapOr404(id);
        sharedMediaStorage.delete(roadmap.getImageUrl());
        roadmapRepository.delete(roadmap);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = sharedMediaStorage.readAsResource(filename);
        if (resource == null) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    private Roadmap createRoadmap(CreateRoadmapRequest request, MultipartFile image) {
        LocalDateTime now = LocalDateTime.now();

        Roadmap roadmap = new Roadmap();
        roadmap.setMainTopic(trim(request.mainTopic()));
        roadmap.setRouterLink(trimNullable(request.routerLink()));
        roadmap.setIntro(trimNullable(request.intro()));
        roadmap.setImageUrl(sharedMediaStorage.save(image));
        roadmap.setCreatedAt(now);
        roadmap.setUpdatedAt(now);

        if (request.chapters() != null) {
            for (RoadmapChapterRequest chapterReq : request.chapters()) {
                RoadmapChapter chapter = new RoadmapChapter();
                chapter.setChapterName(trim(chapterReq.chapterName()));
                chapter.setRoadmap(roadmap);

                if (chapterReq.subtopics() != null) {
                    for (RoadmapSubtopicRequest subtopicReq : chapterReq.subtopics()) {
                        RoadmapSubtopic subtopic = new RoadmapSubtopic();
                        subtopic.setSubtopicName(trim(subtopicReq.subtopicName()));
                        subtopic.setChapter(chapter);
                        chapter.getSubtopics().add(subtopic);
                    }
                }
                roadmap.getChapters().add(chapter);
            }
        }

        return roadmapRepository.save(roadmap);
    }

    private Roadmap updateRoadmap(Long id, UpdateRoadmapRequest request, MultipartFile image) {
        Roadmap roadmap = getRoadmapOr404(id);

        if (request.mainTopic() != null) {
            roadmap.setMainTopic(trim(request.mainTopic()));
        }
        if (request.routerLink() != null) {
            roadmap.setRouterLink(trimNullable(request.routerLink()));
        }
        if (request.intro() != null) {
            roadmap.setIntro(trimNullable(request.intro()));
        }

        if (image != null && !image.isEmpty()) {
            sharedMediaStorage.delete(roadmap.getImageUrl());
            roadmap.setImageUrl(sharedMediaStorage.save(image));
        }

        if (request.chapters() != null) {
            roadmap.getChapters().clear();
            for (RoadmapChapterRequest chapterReq : request.chapters()) {
                RoadmapChapter chapter = new RoadmapChapter();
                chapter.setChapterName(trim(chapterReq.chapterName()));
                chapter.setRoadmap(roadmap);

                if (chapterReq.subtopics() != null) {
                    for (RoadmapSubtopicRequest subtopicReq : chapterReq.subtopics()) {
                        RoadmapSubtopic subtopic = new RoadmapSubtopic();
                        subtopic.setSubtopicName(trim(subtopicReq.subtopicName()));
                        subtopic.setChapter(chapter);
                        chapter.getSubtopics().add(subtopic);
                    }
                }
                roadmap.getChapters().add(chapter);
            }
        }

        roadmap.setUpdatedAt(LocalDateTime.now());
        return roadmapRepository.save(roadmap);
    }

    private Roadmap getRoadmapOr404(Long id) {
        return roadmapRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Roadmap not found"));
    }

    private <T> T parseData(String data, Class<T> targetType) {
        try {
            return objectMapper.readValue(data, targetType);
        } catch (IOException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid multipart data");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    @GetMapping("/import-syllabus/list")
    public ResponseEntity<?> getImportSyllabusList() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/interview/syllabus/public/list"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(httpResponse.body());
            } else {
                return ResponseEntity.status(httpResponse.statusCode()).body(httpResponse.body());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("detail", "Failed to contact syllabus service: " + e.getMessage()));
        }
    }

    @PostMapping("/import-syllabus/{id}")
    public ResponseEntity<?> importSyllabus(@PathVariable Long id) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/interview/syllabus/public/" + id))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                return ResponseEntity.status(httpResponse.statusCode()).body(httpResponse.body());
            }

            // Parse the response
            Map<String, Object> syllabusMap = objectMapper.readValue(httpResponse.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String topic = (String) syllabusMap.get("topic");
            List<Map<String, Object>> chaptersList = (List<Map<String, Object>>) syllabusMap.get("syllabus");

            if (topic == null || topic.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("detail", "Invalid syllabus topic"));
            }

            // Transform to Roadmap
            List<RoadmapChapterRequest> chapterRequests = new ArrayList<>();
            if (chaptersList != null) {
                for (Map<String, Object> chMap : chaptersList) {
                    String title = (String) chMap.get("title");
                    List<String> subtopicsList = (List<String>) chMap.get("subtopics");
                    
                    List<RoadmapSubtopicRequest> subtopicRequests = new ArrayList<>();
                    if (subtopicsList != null) {
                       for (String subtopicName : subtopicsList) {
                           subtopicRequests.add(new RoadmapSubtopicRequest(subtopicName));
                       }
                    }
                    chapterRequests.add(new RoadmapChapterRequest(title, subtopicRequests));
                }
            }

            String routerLink = topic.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            CreateRoadmapRequest createRequest = new CreateRoadmapRequest(
                    topic,
                    chapterRequests,
                    routerLink,
                    "Curated learning path."
            );

            Roadmap created = createRoadmap(createRequest, null);
            return ResponseEntity.status(CREATED).body(RoadmapResponse.fromEntity(created));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("detail", "Failed to import syllabus: " + e.getMessage()));
        }
    }
}
