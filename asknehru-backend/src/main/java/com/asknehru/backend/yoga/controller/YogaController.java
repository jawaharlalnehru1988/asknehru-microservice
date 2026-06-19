package com.asknehru.backend.yoga.controller;

import com.asknehru.backend.yoga.api.YogaDtos.PranayamaArticleRequest;
import com.asknehru.backend.yoga.api.YogaDtos.PranayamaArticleResponse;
import com.asknehru.backend.yoga.api.YogaDtos.PranayamaSequenceRequest;
import com.asknehru.backend.yoga.api.YogaDtos.PranayamaSequenceResponse;
import com.asknehru.backend.yoga.api.YogaDtos.YogaPoseRequest;
import com.asknehru.backend.yoga.api.YogaDtos.YogaPoseResponse;
import com.asknehru.backend.yoga.api.YogaDtos.YogaSequenceRequest;
import com.asknehru.backend.yoga.api.YogaDtos.YogaSequenceResponse;
import com.asknehru.backend.yoga.model.PranayamaArticle;
import com.asknehru.backend.yoga.model.PranayamaSequence;
import com.asknehru.backend.yoga.model.YogaPose;
import com.asknehru.backend.yoga.model.YogaSequence;
import com.asknehru.backend.yoga.repository.PranayamaArticleRepository;
import com.asknehru.backend.yoga.repository.PranayamaSequenceRepository;
import com.asknehru.backend.yoga.repository.YogaPoseRepository;
import com.asknehru.backend.yoga.repository.YogaSequenceRepository;
import com.asknehru.backend.yoga.service.YogaMediaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/yoga")
public class YogaController {

    private static final String CATEGORY_SEQUENCE = "sequence";
    private static final String CATEGORY_PRANAYAMA = "pranayama";

    private final YogaPoseRepository yogaPoseRepository;
    private final YogaSequenceRepository yogaSequenceRepository;
    private final PranayamaSequenceRepository pranayamaSequenceRepository;
    private final PranayamaArticleRepository pranayamaArticleRepository;
    private final YogaMediaService yogaMediaService;

    public YogaController(
            YogaPoseRepository yogaPoseRepository,
            YogaSequenceRepository yogaSequenceRepository,
            PranayamaSequenceRepository pranayamaSequenceRepository,
            PranayamaArticleRepository pranayamaArticleRepository,
            YogaMediaService yogaMediaService
    ) {
        this.yogaPoseRepository = yogaPoseRepository;
        this.yogaSequenceRepository = yogaSequenceRepository;
        this.pranayamaSequenceRepository = pranayamaSequenceRepository;
        this.pranayamaArticleRepository = pranayamaArticleRepository;
        this.yogaMediaService = yogaMediaService;
    }

    @GetMapping("/poses")
    public List<YogaPoseResponse> listYogaPoses(@RequestParam(required = false) String category) {
        List<YogaPose> poses = category == null || category.isBlank()
                ? yogaPoseRepository.findByCategoryNotOrderByIdAsc(CATEGORY_SEQUENCE)
                : yogaPoseRepository.findByCategoryNotAndCategoryIgnoreCaseOrderByIdAsc(CATEGORY_SEQUENCE, category);
        return poses.stream().map(YogaPoseResponse::fromEntity).toList();
    }

    @PostMapping("/poses")
    public ResponseEntity<YogaPoseResponse> createYogaPose(@Valid @RequestBody YogaPoseRequest request) {
        if (CATEGORY_SEQUENCE.equalsIgnoreCase(trim(request.category()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create sequences via /yoga/sequences.");
        }
        YogaPose pose = new YogaPose();
        applyToPose(pose, request);
        YogaPose created = yogaPoseRepository.save(pose);
        return ResponseEntity.status(HttpStatus.CREATED).body(YogaPoseResponse.fromEntity(created));
    }

    @GetMapping("/poses/{id}")
    public YogaPoseResponse getYogaPose(@PathVariable Long id) {
        YogaPose pose = getPoseOr404(id);
        return YogaPoseResponse.fromEntity(pose);
    }

    @PutMapping("/poses/{id}")
    public YogaPoseResponse updateYogaPose(@PathVariable Long id, @Valid @RequestBody YogaPoseRequest request) {
        if (CATEGORY_SEQUENCE.equalsIgnoreCase(trim(request.category()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Update sequences via /yoga/sequences/<id>.");
        }
        YogaPose pose = getPoseOr404(id);

        yogaMediaService.deleteReplacedMedia(pose.getAudioUrl(), request.audioURL(), yogaMediaService.audioDir(), "/media/yoga-audio/");
        yogaMediaService.deleteReplacedMedia(pose.getImageUrl(), request.imageURL(), yogaMediaService.imageDir(), "/media/yoga-poses/");
        yogaMediaService.deleteReplacedMedia(pose.getVideoUrl(), request.videoURL(), yogaMediaService.videoDir(), "/media/yoga-video/");

        applyToPose(pose, request);
        YogaPose updated = yogaPoseRepository.save(pose);
        return YogaPoseResponse.fromEntity(updated);
    }

    @DeleteMapping("/poses/{id}")
    public ResponseEntity<Void> deleteYogaPose(@PathVariable Long id) {
        YogaPose pose = getPoseOr404(id);
        yogaMediaService.deleteManagedMedia(pose.getAudioUrl(), yogaMediaService.audioDir(), "/media/yoga-audio/");
        yogaMediaService.deleteManagedMedia(pose.getImageUrl(), yogaMediaService.imageDir(), "/media/yoga-poses/");
        yogaMediaService.deleteManagedMedia(pose.getVideoUrl(), yogaMediaService.videoDir(), "/media/yoga-video/");
        yogaPoseRepository.delete(pose);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/poses/search")
    public List<YogaPoseResponse> searchYogaPoses(
            @RequestParam(name = "yogaName", defaultValue = "") String yogaName,
            @RequestParam(required = false) String category
    ) {
        List<YogaPose> poses = category == null || category.isBlank()
                ? yogaPoseRepository.findByYogaNameContainingIgnoreCaseAndCategoryNotOrderByIdAsc(yogaName, CATEGORY_SEQUENCE)
                : yogaPoseRepository.findByYogaNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(yogaName, category);
        return poses.stream().map(YogaPoseResponse::fromEntity).toList();
    }

    @GetMapping("/sequences")
    public List<YogaSequenceResponse> listYogaSequences(@RequestParam(required = false) String category) {
        List<YogaSequence> sequences = category == null || category.isBlank()
                ? yogaSequenceRepository.findAllByOrderByIdAsc()
                : yogaSequenceRepository.findByCategoryIgnoreCaseOrderByIdAsc(category);
        return sequences.stream().map(YogaSequenceResponse::fromEntity).toList();
    }

    @PostMapping("/sequences")
    public ResponseEntity<YogaSequenceResponse> createYogaSequence(@Valid @RequestBody YogaSequenceRequest request) {
        YogaSequence sequence = new YogaSequence();
        applyToSequence(sequence, request);
        if (sequence.getCategory() == null || sequence.getCategory().isBlank()) {
            sequence.setCategory(CATEGORY_SEQUENCE);
        }
        YogaSequence created = yogaSequenceRepository.save(sequence);
        return ResponseEntity.status(HttpStatus.CREATED).body(YogaSequenceResponse.fromEntity(created));
    }

    @GetMapping("/sequences/{id}")
    public YogaSequenceResponse getYogaSequence(@PathVariable Long id) {
        return YogaSequenceResponse.fromEntity(getSequenceOr404(id));
    }

    @PutMapping("/sequences/{id}")
    public YogaSequenceResponse updateYogaSequence(@PathVariable Long id, @Valid @RequestBody YogaSequenceRequest request) {
        YogaSequence sequence = getSequenceOr404(id);

        yogaMediaService.deleteReplacedMedia(sequence.getAudioUrl(), request.audioURL(), yogaMediaService.audioDir(), "/media/yoga-audio/");
        yogaMediaService.deleteReplacedMedia(sequence.getImageUrl(), request.imageURL(), yogaMediaService.imageDir(), "/media/yoga-poses/");
        yogaMediaService.deleteReplacedMedia(sequence.getVideoUrl(), request.videoURL(), yogaMediaService.videoDir(), "/media/yoga-video/");

        applyToSequence(sequence, request);
        if (sequence.getCategory() == null || sequence.getCategory().isBlank()) {
            sequence.setCategory(CATEGORY_SEQUENCE);
        }

        YogaSequence updated = yogaSequenceRepository.save(sequence);
        return YogaSequenceResponse.fromEntity(updated);
    }

    @DeleteMapping("/sequences/{id}")
    public ResponseEntity<Void> deleteYogaSequence(@PathVariable Long id) {
        YogaSequence sequence = getSequenceOr404(id);
        yogaMediaService.deleteManagedMedia(sequence.getAudioUrl(), yogaMediaService.audioDir(), "/media/yoga-audio/");
        yogaMediaService.deleteManagedMedia(sequence.getImageUrl(), yogaMediaService.imageDir(), "/media/yoga-poses/");
        yogaMediaService.deleteManagedMedia(sequence.getVideoUrl(), yogaMediaService.videoDir(), "/media/yoga-video/");
        yogaSequenceRepository.delete(sequence);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sequences/search")
    public List<YogaSequenceResponse> searchYogaSequences(
            @RequestParam(name = "sequenceName", defaultValue = "") String sequenceName,
            @RequestParam(required = false) String category
    ) {
        List<YogaSequence> sequences = category == null || category.isBlank()
                ? yogaSequenceRepository.findBySequenceNameContainingIgnoreCaseOrderByIdAsc(sequenceName)
                : yogaSequenceRepository.findBySequenceNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(sequenceName, category);
        return sequences.stream().map(YogaSequenceResponse::fromEntity).toList();
    }

    @GetMapping("/pranayama/sequences")
    public List<PranayamaSequenceResponse> listPranayamaSequences(@RequestParam(required = false) String category) {
        List<PranayamaSequence> sequences = category == null || category.isBlank()
                ? pranayamaSequenceRepository.findAllByOrderByIdAsc()
                : pranayamaSequenceRepository.findByCategoryIgnoreCaseOrderByIdAsc(category);
        return sequences.stream().map(this::toPranayamaSequenceResponse).toList();
    }

    @PostMapping("/pranayama/sequences")
    public ResponseEntity<PranayamaSequenceResponse> createPranayamaSequence(@Valid @RequestBody PranayamaSequenceRequest request) {
        PranayamaSequence sequence = new PranayamaSequence();
        applyToPranayamaSequence(sequence, request);
        if (sequence.getCategory() == null || sequence.getCategory().isBlank()) {
            sequence.setCategory(CATEGORY_PRANAYAMA);
        }
        PranayamaSequence created = pranayamaSequenceRepository.save(sequence);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPranayamaSequenceResponse(created));
    }

    @GetMapping("/pranayama/sequences/{id}")
    public PranayamaSequenceResponse getPranayamaSequence(@PathVariable Long id) {
        return toPranayamaSequenceResponse(getPranayamaSequenceOr404(id));
    }

    @PutMapping("/pranayama/sequences/{id}")
    public PranayamaSequenceResponse updatePranayamaSequence(
            @PathVariable Long id,
            @Valid @RequestBody PranayamaSequenceRequest request
    ) {
        PranayamaSequence sequence = getPranayamaSequenceOr404(id);
        yogaMediaService.deleteReplacedMedia(sequence.getAudioUrl(), request.audioURL(), yogaMediaService.audioDir(), "/media/yoga-audio/");
        applyToPranayamaSequence(sequence, request);
        if (sequence.getCategory() == null || sequence.getCategory().isBlank()) {
            sequence.setCategory(CATEGORY_PRANAYAMA);
        }
        PranayamaSequence updated = pranayamaSequenceRepository.save(sequence);
        return toPranayamaSequenceResponse(updated);
    }

    @DeleteMapping("/pranayama/sequences/{id}")
    public ResponseEntity<Void> deletePranayamaSequence(@PathVariable Long id) {
        PranayamaSequence sequence = getPranayamaSequenceOr404(id);
        yogaMediaService.deleteManagedMedia(sequence.getAudioUrl(), yogaMediaService.audioDir(), "/media/yoga-audio/");
        pranayamaSequenceRepository.delete(sequence);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pranayama/sequences/search")
    public List<PranayamaSequenceResponse> searchPranayamaSequences(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) String category
    ) {
        List<PranayamaSequence> sequences = category == null || category.isBlank()
                ? pranayamaSequenceRepository.findByNameContainingIgnoreCaseOrderByIdAsc(name)
                : pranayamaSequenceRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(name, category);
        return sequences.stream().map(this::toPranayamaSequenceResponse).toList();
    }

    @GetMapping("/pranayama/articles")
    public List<PranayamaArticleResponse> listPranayamaArticles(@RequestParam(required = false) String category) {
        List<PranayamaArticle> articles = category == null || category.isBlank()
                ? pranayamaArticleRepository.findAllByOrderByIdAsc()
                : pranayamaArticleRepository.findByCategoryIgnoreCaseOrderByIdAsc(category);
        return articles.stream().map(PranayamaArticleResponse::fromEntity).toList();
    }

    @PostMapping("/pranayama/articles")
    public ResponseEntity<PranayamaArticleResponse> createPranayamaArticle(@Valid @RequestBody PranayamaArticleRequest request) {
        PranayamaArticle article = new PranayamaArticle();
        applyToPranayamaArticle(article, request);
        PranayamaArticle created = pranayamaArticleRepository.save(article);
        return ResponseEntity.status(HttpStatus.CREATED).body(PranayamaArticleResponse.fromEntity(created));
    }

    @GetMapping("/pranayama/articles/{id}")
    public PranayamaArticleResponse getPranayamaArticle(@PathVariable Long id) {
        return PranayamaArticleResponse.fromEntity(getPranayamaArticleOr404(id));
    }

    @PutMapping("/pranayama/articles/{id}")
    public PranayamaArticleResponse updatePranayamaArticle(
            @PathVariable Long id,
            @Valid @RequestBody PranayamaArticleRequest request
    ) {
        PranayamaArticle article = getPranayamaArticleOr404(id);
        applyToPranayamaArticle(article, request);
        PranayamaArticle updated = pranayamaArticleRepository.save(article);
        return PranayamaArticleResponse.fromEntity(updated);
    }

    @DeleteMapping("/pranayama/articles/{id}")
    public ResponseEntity<Void> deletePranayamaArticle(@PathVariable Long id) {
        PranayamaArticle article = getPranayamaArticleOr404(id);
        pranayamaArticleRepository.delete(article);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pranayama/articles/search")
    public List<PranayamaArticleResponse> searchPranayamaArticles(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) String category
    ) {
        List<PranayamaArticle> articles = category == null || category.isBlank()
                ? pranayamaArticleRepository.findByTitleContainingIgnoreCaseOrderByIdAsc(title)
                : pranayamaArticleRepository.findByTitleContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(title, category);
        return articles.stream().map(PranayamaArticleResponse::fromEntity).toList();
    }

    private YogaPose getPoseOr404(Long id) {
        YogaPose pose = yogaPoseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yoga pose not found with id: " + id));
        if (CATEGORY_SEQUENCE.equalsIgnoreCase(trim(pose.getCategory()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Yoga pose not found with id: " + id);
        }
        return pose;
    }

    private YogaSequence getSequenceOr404(Long id) {
        return yogaSequenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yoga sequence not found with id: " + id));
    }

    private PranayamaSequence getPranayamaSequenceOr404(Long id) {
        return pranayamaSequenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pranayama sequence not found with id: " + id));
    }

    private PranayamaArticle getPranayamaArticleOr404(Long id) {
        return pranayamaArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pranayama article not found with id: " + id));
    }

    private void applyToPose(YogaPose pose, YogaPoseRequest request) {
        pose.setYogaName(trim(request.yogaName()));
        pose.setYogaNameEnglish(trimNullable(request.yogaNameEnglish()));
        pose.setBlogContent(trimNullable(request.blogContent()));
        pose.setAudioUrl(trimNullable(request.audioURL()));
        pose.setVideoUrl(trimNullable(request.videoURL()));
        pose.setImageUrl(trimNullable(request.imageURL()));
        pose.setCategory(trimNullable(request.category()));
    }

    private void applyToSequence(YogaSequence sequence, YogaSequenceRequest request) {
        sequence.setSequenceName(trim(request.sequenceName()));
        sequence.setBlogContent(trimNullable(request.blogContent()));
        sequence.setAudioUrl(trimNullable(request.audioURL()));
        sequence.setVideoUrl(trimNullable(request.videoURL()));
        sequence.setImageUrl(trimNullable(request.imageURL()));
        sequence.setCategory(trimNullable(request.category()));
    }

    private void applyToPranayamaSequence(PranayamaSequence sequence, PranayamaSequenceRequest request) {
        sequence.setName(trim(request.name()));
        sequence.setDescription(trim(request.description()));
        sequence.setSteps(trim(request.steps()));
        sequence.setDuration(trimNullable(request.duration()));
        sequence.setBenefits(trimNullable(request.benefits()));
        sequence.setContraindications(trimNullable(request.contraindications()));
        sequence.setAudioUrl(trimNullable(request.audioURL()));
        sequence.setCategory(trimNullable(request.category()));
    }

    private void applyToPranayamaArticle(PranayamaArticle article, PranayamaArticleRequest request) {
        if (request.pranayamaSequenceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pranayama_sequence is required");
        }
        article.setPranayamaSequenceId(request.pranayamaSequenceId());
        article.setTitle(trim(request.title()));
        article.setContent(trim(request.content()));
        article.setCategory(trimNullable(request.category()));
    }

    private PranayamaSequenceResponse toPranayamaSequenceResponse(PranayamaSequence sequence) {
        List<PranayamaArticleResponse> relatedArticles = pranayamaArticleRepository
                .findByPranayamaSequenceIdOrderByIdAsc(sequence.getId())
                .stream()
                .map(PranayamaArticleResponse::fromEntity)
                .toList();
        return PranayamaSequenceResponse.fromEntity(sequence, relatedArticles);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}
