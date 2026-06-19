package com.asknehru.backend.yoga.repository;

import com.asknehru.backend.yoga.model.YogaPose;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YogaPoseRepository extends JpaRepository<YogaPose, Long> {

    List<YogaPose> findByCategoryIgnoreCaseOrderByIdAsc(String category);

    List<YogaPose> findByYogaNameContainingIgnoreCaseAndCategoryNotOrderByIdAsc(String yogaName, String excludedCategory);

    List<YogaPose> findByYogaNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(String yogaName, String category);

    List<YogaPose> findByCategoryNotOrderByIdAsc(String excludedCategory);

    List<YogaPose> findByCategoryNotAndCategoryIgnoreCaseOrderByIdAsc(String excludedCategory, String category);
}
