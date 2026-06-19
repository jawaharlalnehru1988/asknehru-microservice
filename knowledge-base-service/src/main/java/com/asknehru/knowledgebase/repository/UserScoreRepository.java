package com.asknehru.knowledgebase.repository;

import com.asknehru.knowledgebase.model.UserScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, Long> {
    List<UserScore> findByUserId(Long userId);
    Optional<UserScore> findByUserIdAndSubtopicId(Long userId, Long subtopicId);
}
