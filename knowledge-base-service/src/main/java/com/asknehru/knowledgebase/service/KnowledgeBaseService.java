package com.asknehru.knowledgebase.service;

import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.CreateKnowledgeBaseRequest;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.UpdateKnowledgeBaseRequest;
import com.asknehru.knowledgebase.api.KnowledgeBaseDtos.KnowledgeBaseResponse;
import com.asknehru.knowledgebase.model.KnowledgeBase;
import com.asknehru.knowledgebase.repository.KnowledgeBaseRepository;
import com.asknehru.knowledgebase.util.SharedMediaStorage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.asknehru.knowledgebase.api.ScoreDtos.SaveScoreRequest;
import com.asknehru.knowledgebase.api.ScoreDtos.UserScoreResponse;
import com.asknehru.knowledgebase.model.UserScore;
import com.asknehru.knowledgebase.repository.UserScoreRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final SharedMediaStorage sharedMediaStorage;
    private final LlmService llmService;
    private final UserScoreRepository userScoreRepository;

    public KnowledgeBaseService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            @Qualifier("knowledgeBaseSharedMediaStorage") SharedMediaStorage sharedMediaStorage,
            LlmService llmService,
            UserScoreRepository userScoreRepository
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.sharedMediaStorage = sharedMediaStorage;
        this.llmService = llmService;
        this.userScoreRepository = userScoreRepository;
    }

    public List<String> getDistinctMainTopics() {
        return knowledgeBaseRepository.findDistinctMainTopics();
    }

    public List<Long> getExplainedSubtopicIds() {
        return knowledgeBaseRepository.findAll().stream()
                .map(KnowledgeBase::getSubtopicId)
                .toList();
    }

    public KnowledgeBase explainSubtopic(Long subtopicId) {
        Optional<KnowledgeBase> cached = knowledgeBaseRepository.findBySubtopicId(subtopicId);
        if (cached.isPresent()) {
            return cached.get();
        }

        KnowledgeBaseRepository.SubtopicContextProjection context = knowledgeBaseRepository.findContextBySubtopicId(subtopicId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtopic context not found for ID: " + subtopicId);
        }

        String systemPrompt = "You are a professional software engineering tutor and domain expert. " +
                "Provide a comprehensive, high-quality, professional explanation of the subtopic in markdown format. " +
                "Include code snippets where relevant, structure with clear headers, and keep it practical and concise.";
        String userPrompt = String.format("Explain the subtopic '%s' in the context of the main topic '%s'.",
                context.getSubTopicName(), context.getMainTopic());

        String article = llmService.generate(systemPrompt, userPrompt);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setSubtopicId(subtopicId);
        knowledgeBase.setArticle(article);
        knowledgeBase.setCreatedAt(Instant.now());
        knowledgeBase.setUpdatedAt(Instant.now());

        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBaseRepository.findAllByOrderByIdAsc();
    }

    public KnowledgeBase createKnowledgeBase(CreateKnowledgeBaseRequest request, MultipartFile articleAudio) {
        Instant now = Instant.now();

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        Long subtopicId = request.subtopicId();
        if (subtopicId == null && request.mainTopic() != null && request.subTopic() != null) {
            subtopicId = knowledgeBaseRepository.findSubtopicIdByNames(request.subTopic(), request.mainTopic());
        }
        knowledgeBase.setSubtopicId(subtopicId);
        knowledgeBase.setArticle(trim(request.article()));
        knowledgeBase.setArticleAudio(sharedMediaStorage.save(articleAudio));
        knowledgeBase.setCreatedAt(now);
        knowledgeBase.setUpdatedAt(now);

        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public KnowledgeBase getKnowledgeBaseOr404(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KnowledgeBase not found"));
    }

    public KnowledgeBase updateKnowledgeBase(Long id, UpdateKnowledgeBaseRequest request, MultipartFile articleAudio) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOr404(id);

        Long subtopicId = request.subtopicId();
        if (subtopicId == null && request.mainTopic() != null && request.subTopic() != null) {
            subtopicId = knowledgeBaseRepository.findSubtopicIdByNames(request.subTopic(), request.mainTopic());
        }
        if (subtopicId != null) {
            knowledgeBase.setSubtopicId(subtopicId);
        }
        if (request.article() != null) {
            knowledgeBase.setArticle(trim(request.article()));
        }

        if (articleAudio != null && !articleAudio.isEmpty()) {
            sharedMediaStorage.delete(knowledgeBase.getArticleAudio());
            knowledgeBase.setArticleAudio(sharedMediaStorage.save(articleAudio));
        }

        knowledgeBase.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public void deleteKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOr404(id);
        sharedMediaStorage.delete(knowledgeBase.getArticleAudio());
        knowledgeBaseRepository.delete(knowledgeBase);
    }

    public KnowledgeBase generateMcqs(Long id) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOr404(id);

        String systemPrompt = "You are a professional software engineering tutor and domain expert. " +
                "Generate exactly 10 multiple-choice questions (MCQs) to evaluate the user's understanding of the provided article. " +
                "For each question, provide 4 options and specify the correct option index (0 to 3) or label. " +
                "Ensure that the correct answer index (answerIndex) is randomly and evenly distributed across indices 0, 1, 2, and 3 (i.e. do not bias towards placing most correct answers under index 0 or 1). " +
                "You must output valid JSON format ONLY. Do not wrap in markdown code blocks or any other formatting. " +
                "The JSON schema must be an array of objects: " +
                "[{\"question\": \"...\", \"options\": [\"opt0\", \"opt1\", \"opt2\", \"opt3\"], \"answerIndex\": 0}]";

        String userPrompt = "Generate 10 MCQs for this article:\n\n" + knowledgeBase.getArticle();

        String mcqJson = llmService.generate(systemPrompt, userPrompt);

        // Clean the JSON string if LLM wraps in markdown backticks
        if (mcqJson.contains("```json")) {
            mcqJson = mcqJson.substring(mcqJson.indexOf("```json") + 7);
            if (mcqJson.contains("```")) {
                mcqJson = mcqJson.substring(0, mcqJson.indexOf("```"));
            }
        } else if (mcqJson.contains("```")) {
            mcqJson = mcqJson.substring(mcqJson.indexOf("```") + 3);
            if (mcqJson.contains("```")) {
                mcqJson = mcqJson.substring(0, mcqJson.indexOf("```"));
            }
        }
        mcqJson = mcqJson.trim();

        knowledgeBase.setMcqs(mcqJson);
        knowledgeBase.setUpdatedAt(Instant.now());

        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        if (kb == null) return null;
        String mainTopic = null;
        String subTopic = null;
        if (kb.getSubtopicId() != null) {
            KnowledgeBaseRepository.SubtopicContextProjection context = knowledgeBaseRepository.findContextBySubtopicId(kb.getSubtopicId());
            if (context != null) {
                mainTopic = context.getMainTopic();
                subTopic = context.getSubTopicName();
            }
        }
        return KnowledgeBaseResponse.fromEntity(kb, mainTopic, subTopic);
    }

    public UserScoreResponse saveUserScore(Long userId, Long subtopicId, SaveScoreRequest request) {
        KnowledgeBaseRepository.SubtopicContextProjection context = knowledgeBaseRepository.findContextBySubtopicId(subtopicId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtopic context not found for ID: " + subtopicId);
        }

        UserScore score = userScoreRepository.findByUserIdAndSubtopicId(userId, subtopicId).orElse(new UserScore());
        score.setUserId(userId);
        score.setSubtopicId(subtopicId);
        score.setRoadmapId(context.getRoadmapId());
        score.setScore(request.score());
        score.setTotalQuestions(request.totalQuestions());
        score.setCompletedAt(Instant.now());

        UserScore saved = userScoreRepository.save(score);
        return UserScoreResponse.fromEntity(saved, context.getMainTopic(), context.getSubTopicName());
    }

    public List<UserScoreResponse> getUserScores(Long userId) {
        return userScoreRepository.findByUserId(userId).stream().map(score -> {
            KnowledgeBaseRepository.SubtopicContextProjection context = knowledgeBaseRepository.findContextBySubtopicId(score.getSubtopicId());
            String mainTopic = context != null ? context.getMainTopic() : null;
            String subTopic = context != null ? context.getSubTopicName() : null;
            return UserScoreResponse.fromEntity(score, mainTopic, subTopic);
        }).toList();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
