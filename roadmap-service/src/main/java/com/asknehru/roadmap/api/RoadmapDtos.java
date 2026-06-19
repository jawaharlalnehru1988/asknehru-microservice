package com.asknehru.roadmap.api;

import com.asknehru.roadmap.model.Roadmap;
import com.asknehru.roadmap.model.RoadmapChapter;
import com.asknehru.roadmap.model.RoadmapSubtopic;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class RoadmapDtos {

    private RoadmapDtos() {
    }

    public record RoadmapSubtopicResponse(
            Long id,
            String subtopicName
    ) {
        public static RoadmapSubtopicResponse fromEntity(RoadmapSubtopic subtopic) {
            return new RoadmapSubtopicResponse(subtopic.getId(), subtopic.getSubtopicName());
        }
    }

    public record RoadmapChapterResponse(
            Long id,
            String chapterName,
            List<RoadmapSubtopicResponse> subtopics
    ) {
        public static RoadmapChapterResponse fromEntity(RoadmapChapter chapter) {
            return new RoadmapChapterResponse(
                    chapter.getId(),
                    chapter.getChapterName(),
                    chapter.getSubtopics() != null ? chapter.getSubtopics().stream().map(RoadmapSubtopicResponse::fromEntity).toList() : List.of()
            );
        }
    }

    public record RoadmapResponse(
            Long id,
            String mainTopic,
            List<RoadmapChapterResponse> chapters,
            String imageUrl,
            String routerLink,
            String intro,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static RoadmapResponse fromEntity(Roadmap roadmap) {
            return new RoadmapResponse(
                    roadmap.getId(),
                    roadmap.getMainTopic(),
                    roadmap.getChapters() != null ? roadmap.getChapters().stream().map(RoadmapChapterResponse::fromEntity).toList() : List.of(),
                    roadmap.getImageUrl() == null ? null : "/api/roadmaps/images/" + roadmap.getImageUrl(),
                    roadmap.getRouterLink(),
                    roadmap.getIntro(),
                    roadmap.getCreatedAt(),
                    roadmap.getUpdatedAt()
            );
        }
    }

    public record RoadmapSubtopicRequest(
            @NotBlank String subtopicName
    ) {
    }

    public record RoadmapChapterRequest(
            @NotBlank @Size(max = 500) String chapterName,
            List<RoadmapSubtopicRequest> subtopics
    ) {
    }

    public record CreateRoadmapRequest(
            @NotBlank @Size(max = 200) String mainTopic,
            List<RoadmapChapterRequest> chapters,
            String routerLink,
            @Size(max = 100) String intro
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UpdateRoadmapRequest(
            @Size(max = 200) String mainTopic,
            List<RoadmapChapterRequest> chapters,
            String routerLink,
            @Size(max = 100) String intro
    ) {
    }
}
