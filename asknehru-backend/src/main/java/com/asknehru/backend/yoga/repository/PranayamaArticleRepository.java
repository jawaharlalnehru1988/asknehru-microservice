package com.asknehru.backend.yoga.repository;

import com.asknehru.backend.yoga.model.PranayamaArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PranayamaArticleRepository extends JpaRepository<PranayamaArticle, Long> {

    List<PranayamaArticle> findByPranayamaSequenceIdOrderByIdAsc(Long pranayamaSequenceId);

    List<PranayamaArticle> findByTitleContainingIgnoreCaseOrderByIdAsc(String title);

    List<PranayamaArticle> findByTitleContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(String title, String category);

    List<PranayamaArticle> findByCategoryIgnoreCaseOrderByIdAsc(String category);

    List<PranayamaArticle> findAllByOrderByIdAsc();
}
