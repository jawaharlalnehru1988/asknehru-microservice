package com.asknehru.backend.yoga.repository;

import com.asknehru.backend.yoga.model.PranayamaSequence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PranayamaSequenceRepository extends JpaRepository<PranayamaSequence, Long> {

    List<PranayamaSequence> findByCategoryIgnoreCaseOrderByIdAsc(String category);

    List<PranayamaSequence> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<PranayamaSequence> findByNameContainingIgnoreCaseAndCategoryIgnoreCaseOrderByIdAsc(String name, String category);

    List<PranayamaSequence> findAllByOrderByIdAsc();
}
