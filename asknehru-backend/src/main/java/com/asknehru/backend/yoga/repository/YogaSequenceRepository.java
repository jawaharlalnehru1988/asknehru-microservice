package com.asknehru.backend.yoga.repository;

import com.asknehru.backend.yoga.model.YogaSequence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YogaSequenceRepository extends JpaRepository<YogaSequence, Long> {

    List<YogaSequence> findByCategoryIgnoreCaseOrderByIdAsc(String category);

    List<YogaSequence> findBySequenceNameContainingIgnoreCaseOrderByIdAsc(String sequenceName);

    List<YogaSequence> findBySequenceNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(String sequenceName, String category);

    List<YogaSequence> findAllByOrderByIdAsc();
}
