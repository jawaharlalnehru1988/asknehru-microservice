package com.asknehru.knowledgebase.controller;

import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.KnowledgeBaseResponse;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.CreateKnowledgeBaseRequest;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.MainTopic;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.UpdateKnowledgeBaseRequest;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.ChatRequest;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.ChatResponse;
import com.asknehru.knowledgebase.model.KnowledgeBase;
import com.asknehru.knowledgebase.service.KnowledgeBaseService;
import com.asknehru.knowledgebase.util.SharedMediaStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.asknehru.auth.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.asknehru.knowledgebase.api.ScoreDtos.SaveScoreRequest;
import com.asknehru.knowledgebase.api.ScoreDtos.UserScoreResponse;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/conversations")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final SharedMediaStorage sharedMediaStorage;
    private final ObjectMapper objectMapper;
    private final JwtTokenService jwtTokenService;

    public KnowledgeBaseController(
            KnowledgeBaseService knowledgeBaseService,
            @Qualifier("knowledgeBaseSharedMediaStorage") SharedMediaStorage sharedMediaStorage,
            ObjectMapper objectMapper,
            JwtTokenService jwtTokenService
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.sharedMediaStorage = sharedMediaStorage;
        this.objectMapper = objectMapper;
        this.jwtTokenService = jwtTokenService;
    }

    public record ExplainRequest(@NotNull Long subtopicId) {}

    @GetMapping("/main-topics")
    public List<MainTopic> getMainTopics() {
        return knowledgeBaseService.getDistinctMainTopics().stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(topic -> new MainTopic(topic, formatLabel(topic)))
                .toList();
    }

    private String formatLabel(String value) {
        if (value == null || value.isBlank()) return "";
        String[] words = value.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if ("ai".equals(word)) {
                    sb.append("AI");
                } else {
                    sb.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1));
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    @GetMapping("/exists")
    public List<Long> getExplainedSubtopicIds() {
        return knowledgeBaseService.getExplainedSubtopicIds();
    }

    @PostMapping("/explain")
    public ResponseEntity<KnowledgeBaseResponse> explainSubtopic(@Valid @RequestBody ExplainRequest request) {
        KnowledgeBase explained = knowledgeBaseService.explainSubtopic(request.subtopicId());
        return ResponseEntity.ok(knowledgeBaseService.toResponse(explained));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatAboutSubtopic(@Valid @RequestBody ChatRequest request) {
        String answer = knowledgeBaseService.chatAboutSubtopic(request.subtopicId(), request.question());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @GetMapping
    public List<KnowledgeBaseResponse> listKnowledgeBases() {
        return knowledgeBaseService.listKnowledgeBases().stream()
                .map(knowledgeBaseService::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KnowledgeBaseResponse> createKnowledgeBaseJson(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBase created = knowledgeBaseService.createKnowledgeBase(request, null);
        return ResponseEntity.status(CREATED).body(knowledgeBaseService.toResponse(created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KnowledgeBaseResponse> createKnowledgeBaseMultipart(
            @RequestPart("data") String data,
            @RequestPart(value = "articleAudio", required = false) MultipartFile articleAudio
    ) {
        CreateKnowledgeBaseRequest request = parseData(data, CreateKnowledgeBaseRequest.class);
        KnowledgeBase created = knowledgeBaseService.createKnowledgeBase(request, articleAudio);
        return ResponseEntity.status(CREATED).body(knowledgeBaseService.toResponse(created));
    }

    @GetMapping("/{id}")
    public KnowledgeBaseResponse getKnowledgeBase(@PathVariable Long id) {
        return knowledgeBaseService.toResponse(knowledgeBaseService.getKnowledgeBaseOr404(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public KnowledgeBaseResponse updateKnowledgeBaseJson(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request
    ) {
        KnowledgeBase updated = knowledgeBaseService.updateKnowledgeBase(id, request, null);
        return knowledgeBaseService.toResponse(updated);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KnowledgeBaseResponse updateKnowledgeBaseMultipart(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "articleAudio", required = false) MultipartFile articleAudio
    ) {
        UpdateKnowledgeBaseRequest request = parseData(data, UpdateKnowledgeBaseRequest.class);
        KnowledgeBase updated = knowledgeBaseService.updateKnowledgeBase(id, request, articleAudio);
        return knowledgeBaseService.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mcq")
    public ResponseEntity<KnowledgeBaseResponse> generateMcqs(@PathVariable Long id) {
        KnowledgeBase saved = knowledgeBaseService.generateMcqs(id);
        return ResponseEntity.ok(knowledgeBaseService.toResponse(saved));
    }

    @GetMapping("/audio/{filename}")
    public ResponseEntity<Resource> getAudio(@PathVariable String filename) {
        Resource resource = sharedMediaStorage.readAsResource(filename);
        if (resource == null) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @PostMapping("/{subtopicId}/scores")
    public ResponseEntity<UserScoreResponse> saveScore(
            @PathVariable Long subtopicId,
            @Valid @RequestBody SaveScoreRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromAuthHeader(authHeader);
        UserScoreResponse response = knowledgeBaseService.saveUserScore(userId, subtopicId, request);
        return ResponseEntity.status(CREATED).body(response);
    }

    @GetMapping("/scores")
    public List<UserScoreResponse> getUserScores(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromAuthHeader(authHeader);
        return knowledgeBaseService.getUserScores(userId);
    }

    private Long getUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        try {
            String token = authHeader.substring(7).trim();
            Claims claims = jwtTokenService.parseAndValidate(token);
            if (!"access".equals(claims.get("type", String.class))) {
                throw new ResponseStatusException(UNAUTHORIZED, "Invalid token type");
            }
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | NumberFormatException ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid token");
        }
    }

    private <T> T parseData(String data, Class<T> targetType) {
        try {
            return objectMapper.readValue(data, targetType);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid multipart data");
        }
    }
}
