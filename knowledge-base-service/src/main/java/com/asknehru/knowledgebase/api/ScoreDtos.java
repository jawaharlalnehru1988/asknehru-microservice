package com.asknehru.knowledgebase.api;

import com.asknehru.knowledgebase.model.UserScore;
import java.time.Instant;

public class ScoreDtos {

    public record SaveScoreRequest(
            Integer score,
            Integer totalQuestions
    ) {}

    public record UserScoreResponse(
            Long id,
            Long subtopicId,
            Long roadmapId,
            String mainTopic,
            String chapterName,
            String subTopicName,
            Integer score,
            Integer totalQuestions,
            Instant completedAt
    ) {
        public static UserScoreResponse fromEntity(
                UserScore score,
                String mainTopic,
                String chapterName,
                String subTopicName
        ) {
            return new UserScoreResponse(
                    score.getId(),
                    score.getSubtopicId(),
                    score.getRoadmapId(),
                    mainTopic,
                    chapterName,
                    subTopicName,
                    score.getScore(),
                    score.getTotalQuestions(),
                    score.getCompletedAt()
            );
        }
    }
}
