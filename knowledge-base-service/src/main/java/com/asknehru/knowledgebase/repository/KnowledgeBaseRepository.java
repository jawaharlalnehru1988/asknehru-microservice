package com.asknehru.knowledgebase.repository;

import com.asknehru.knowledgebase.model.KnowledgeBase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findAllByOrderByIdAsc();

    Optional<KnowledgeBase> findBySubtopicId(Long subtopicId);

    @Query(value = "SELECT DISTINCT main_topic FROM roadmaps ORDER BY main_topic ASC", nativeQuery = true)
    List<String> findDistinctMainTopics();

    @Query(value = "SELECT rs.subtopic_name as subTopicName, rc.chapter_name as chapterName, r.main_topic as mainTopic, r.id as roadmapId, r.category as category " +
                   "FROM roadmap_subtopics rs " +
                   "JOIN roadmap_chapters rc ON rs.chapter_id = rc.id " +
                   "JOIN roadmaps r ON rc.roadmap_id = r.id " +
                   "WHERE rs.id = :subtopicId", nativeQuery = true)
    SubtopicContextProjection findContextBySubtopicId(@Param("subtopicId") Long subtopicId);

    @Query(value = "SELECT rs.id FROM roadmap_subtopics rs " +
                   "JOIN roadmap_chapters rc ON rs.chapter_id = rc.id " +
                   "JOIN roadmaps r ON rc.roadmap_id = r.id " +
                   "WHERE rs.subtopic_name = :subtopicName AND r.main_topic = :mainTopic LIMIT 1", nativeQuery = true)
    Long findSubtopicIdByNames(@Param("subtopicName") String subtopicName, @Param("mainTopic") String mainTopic);

    @Query(value = "SELECT kb.subtopic_id as subtopicId, " +
                   "CASE WHEN kb.mcqs IS NOT NULL AND jsonb_typeof(kb.mcqs::jsonb) = 'array' " +
                   "     THEN jsonb_array_length(kb.mcqs::jsonb) ELSE 0 END as totalQuestions, " +
                   "r.id as roadmapId " +
                   "FROM knowledge_base kb " +
                   "LEFT JOIN roadmap_subtopics rs ON kb.subtopic_id = rs.id " +
                   "LEFT JOIN roadmap_chapters rc ON rs.chapter_id = rc.id " +
                   "LEFT JOIN roadmaps r ON rc.roadmap_id = r.id " +
                   "WHERE kb.subtopic_id IS NOT NULL", nativeQuery = true)
    List<SubtopicMcqSummaryProjection> findSubtopicMcqSummaries();

    interface SubtopicMcqSummaryProjection {
        Long getSubtopicId();
        Integer getTotalQuestions();
        Long getRoadmapId();
    }

    interface SubtopicContextProjection {
        String getSubTopicName();
        String getChapterName();
        String getMainTopic();
        Long getRoadmapId();
        String getCategory();
    }
}
