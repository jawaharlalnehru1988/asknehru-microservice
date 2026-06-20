package com.asknehru.knowledgebase.api;

import com.asknehru.knowledgebase.model.KnowledgeBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class KnowledgeBaseDtos {

    private KnowledgeBaseDtos() {
    }

    public record KnowledgeBaseResponse(
            Long id,
            String article,
            String articleAudio,
            Long subtopicId,
            Instant createdAt,
            Instant updatedAt,
            String mcqs,
            String mainTopic,
            String subTopic
    ) {
        public static KnowledgeBaseResponse fromEntity(KnowledgeBase knowledgeBase) {
            return fromEntity(knowledgeBase, null, null);
        }

        public static KnowledgeBaseResponse fromEntity(KnowledgeBase knowledgeBase, String mainTopic, String subTopic) {
            return new KnowledgeBaseResponse(
                    knowledgeBase.getId(),
                    knowledgeBase.getArticle(),
                    knowledgeBase.getArticleAudio() == null ? null : "/api/conversations/audio/" + knowledgeBase.getArticleAudio(),
                    knowledgeBase.getSubtopicId(),
                    knowledgeBase.getCreatedAt(),
                    knowledgeBase.getUpdatedAt(),
                    knowledgeBase.getMcqs(),
                    mainTopic,
                    subTopic
            );
        }
    }

    public record MainTopic(String value, String label) {
    }

    public record CreateKnowledgeBaseRequest(
            Long subtopicId,
            @NotBlank String article,
            String mainTopic,
            String subTopic
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UpdateKnowledgeBaseRequest(
            Long subtopicId,
            String article,
            String mainTopic,
            String subTopic
    ) {
    }

    public record ChatRequest(
            @NotNull Long subtopicId,
            @NotBlank String question
    ) {
    }

    public record ChatResponse(
            String answer
    ) {
    }
}
