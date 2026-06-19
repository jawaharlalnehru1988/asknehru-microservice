package com.asknehru.backend.yoga.api;

import com.asknehru.backend.yoga.model.PranayamaArticle;
import com.asknehru.backend.yoga.model.PranayamaSequence;
import com.asknehru.backend.yoga.model.YogaPose;
import com.asknehru.backend.yoga.model.YogaSequence;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public final class YogaDtos {

    private YogaDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YogaPoseRequest(
            @JsonProperty("yogaName") @NotBlank String yogaName,
            @JsonProperty("yogaNameEnglish") String yogaNameEnglish,
            @JsonProperty("blogContent") String blogContent,
            @JsonProperty("audioURL") String audioURL,
            @JsonProperty("videoURL") String videoURL,
            @JsonProperty("imageURL") String imageURL,
            @JsonProperty("category") String category
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YogaPoseResponse(
            @JsonProperty("audioURL") String audioURL,
            @JsonProperty("blogContent") String blogContent,
            @JsonProperty("category") String category,
            @JsonProperty("id") Long id,
            @JsonProperty("imageURL") String imageURL,
            @JsonProperty("videoURL") String videoURL,
            @JsonProperty("yogaName") String yogaName,
            @JsonProperty("yogaNameEnglish") String yogaNameEnglish
    ) {
        public static YogaPoseResponse fromEntity(YogaPose pose) {
            return new YogaPoseResponse(
                    nullIfBlank(pose.getAudioUrl()),
                    nullIfBlank(pose.getBlogContent()),
                    nullIfBlank(pose.getCategory()),
                    pose.getId(),
                    nullIfBlank(pose.getImageUrl()),
                    nullIfBlank(pose.getVideoUrl()),
                    nullIfBlank(pose.getYogaName()),
                    nullIfBlank(pose.getYogaNameEnglish())
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YogaSequenceRequest(
            @JsonProperty("sequenceName") @NotBlank String sequenceName,
            @JsonProperty("blogContent") String blogContent,
            @JsonProperty("audioURL") String audioURL,
            @JsonProperty("videoURL") String videoURL,
            @JsonProperty("imageURL") String imageURL,
            @JsonProperty("category") String category
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YogaSequenceResponse(
            @JsonProperty("audioURL") String audioURL,
            @JsonProperty("blogContent") String blogContent,
            @JsonProperty("category") String category,
            @JsonProperty("id") Long id,
            @JsonProperty("imageURL") String imageURL,
            @JsonProperty("sequenceName") String sequenceName,
            @JsonProperty("videoURL") String videoURL
    ) {
        public static YogaSequenceResponse fromEntity(YogaSequence sequence) {
            return new YogaSequenceResponse(
                    nullIfBlank(sequence.getAudioUrl()),
                    nullIfBlank(sequence.getBlogContent()),
                    nullIfBlank(sequence.getCategory()),
                    sequence.getId(),
                    nullIfBlank(sequence.getImageUrl()),
                    nullIfBlank(sequence.getSequenceName()),
                    nullIfBlank(sequence.getVideoUrl())
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PranayamaArticleRequest(
            @JsonProperty("pranayama_sequence") Long pranayamaSequenceId,
            @JsonProperty("title") @NotBlank String title,
            @JsonProperty("content") @NotBlank String content,
            @JsonProperty("category") String category
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PranayamaArticleResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("pranayama_sequence") Long pranayamaSequenceId,
            @JsonProperty("title") String title,
            @JsonProperty("content") String content,
            @JsonProperty("category") String category
    ) {
        public static PranayamaArticleResponse fromEntity(PranayamaArticle article) {
            return new PranayamaArticleResponse(
                    article.getId(),
                    article.getPranayamaSequenceId(),
                    nullIfBlank(article.getTitle()),
                    nullIfBlank(article.getContent()),
                    nullIfBlank(article.getCategory())
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PranayamaSequenceRequest(
            @JsonProperty("name") @NotBlank String name,
            @JsonProperty("description") @NotBlank String description,
            @JsonProperty("steps") @NotBlank String steps,
            @JsonProperty("duration") String duration,
            @JsonProperty("benefits") String benefits,
            @JsonProperty("contraindications") String contraindications,
            @JsonProperty("audioURL") String audioURL,
            @JsonProperty("category") String category
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PranayamaSequenceResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("steps") String steps,
            @JsonProperty("duration") String duration,
            @JsonProperty("benefits") String benefits,
            @JsonProperty("contraindications") String contraindications,
            @JsonProperty("audioURL") String audioURL,
            @JsonProperty("category") String category,
            @JsonProperty("relatedArticles") java.util.List<PranayamaArticleResponse> relatedArticles
    ) {
        public static PranayamaSequenceResponse fromEntity(
                PranayamaSequence sequence,
                java.util.List<PranayamaArticleResponse> relatedArticles
        ) {
            return new PranayamaSequenceResponse(
                    sequence.getId(),
                    nullIfBlank(sequence.getName()),
                    nullIfBlank(sequence.getDescription()),
                    nullIfBlank(sequence.getSteps()),
                    nullIfBlank(sequence.getDuration()),
                    nullIfBlank(sequence.getBenefits()),
                    nullIfBlank(sequence.getContraindications()),
                    nullIfBlank(sequence.getAudioUrl()),
                    nullIfBlank(sequence.getCategory()),
                    relatedArticles
            );
        }
    }

    private static String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        return value.isBlank() ? null : value;
    }
}
