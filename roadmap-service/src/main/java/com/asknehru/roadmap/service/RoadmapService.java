package com.asknehru.roadmap.service;

import com.asknehru.roadmap.api.RoadmapDtos.CreateRoadmapRequest;
import com.asknehru.roadmap.api.RoadmapDtos.RoadmapChapterRequest;
import com.asknehru.roadmap.api.RoadmapDtos.RoadmapSubtopicRequest;
import com.asknehru.roadmap.api.RoadmapDtos.UpdateRoadmapRequest;
import com.asknehru.roadmap.model.Roadmap;
import com.asknehru.roadmap.model.RoadmapChapter;
import com.asknehru.roadmap.model.RoadmapSubtopic;
import com.asknehru.roadmap.repository.RoadmapRepository;
import com.asknehru.roadmap.util.SharedMediaStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final SharedMediaStorage sharedMediaStorage;
    private final ObjectMapper objectMapper;

    public RoadmapService(
            RoadmapRepository roadmapRepository,
            @Qualifier("roadmapSharedMediaStorage") SharedMediaStorage sharedMediaStorage,
            ObjectMapper objectMapper
    ) {
        this.roadmapRepository = roadmapRepository;
        this.sharedMediaStorage = sharedMediaStorage;
        this.objectMapper = objectMapper;
    }

    public List<Roadmap> listRoadmaps() {
        return roadmapRepository.findAllByOrderByCreatedAtDescIdDesc();
    }

    public Roadmap createRoadmap(CreateRoadmapRequest request, MultipartFile image) {
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

    public Roadmap updateRoadmap(Long id, UpdateRoadmapRequest request, MultipartFile image) {
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

    public Roadmap getRoadmapOr404(Long id) {
        return roadmapRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Roadmap not found"));
    }

    public void deleteRoadmap(Long id) {
        Roadmap roadmap = getRoadmapOr404(id);
        sharedMediaStorage.delete(roadmap.getImageUrl());
        roadmapRepository.delete(roadmap);
    }

    public org.springframework.core.io.Resource getImage(String filename) {
        org.springframework.core.io.Resource resource = sharedMediaStorage.readAsResource(filename);
        if (resource == null) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return resource;
    }

    public String getImportSyllabusList() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/interview/syllabus/public/list"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                return httpResponse.body();
            } else {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.valueOf(httpResponse.statusCode()), httpResponse.body());
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to contact syllabus service: " + e.getMessage());
        }
    }

    public Roadmap importSyllabus(Long id) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/interview/syllabus/public/" + id))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.valueOf(httpResponse.statusCode()), httpResponse.body());
            }

            // Parse the response
            Map<String, Object> syllabusMap = objectMapper.readValue(httpResponse.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String topic = (String) syllabusMap.get("topic");
            List<Map<String, Object>> chaptersList = (List<Map<String, Object>>) syllabusMap.get("syllabus");

            if (topic == null || topic.trim().isEmpty()) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid syllabus topic");
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

            return createRoadmap(createRequest, null);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to import syllabus: " + e.getMessage());
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
