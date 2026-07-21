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

    interface SubtopicContextProjection {
        String getSubTopicName();
        String getChapterName();
        String getMainTopic();
        Long getRoadmapId();
        String getCategory();
    }
}
