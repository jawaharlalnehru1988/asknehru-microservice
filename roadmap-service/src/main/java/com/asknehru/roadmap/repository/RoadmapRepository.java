package com.asknehru.roadmap.repository;

import com.asknehru.roadmap.model.Roadmap;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    List<Roadmap> findAllByOrderByCreatedAtDescIdDesc();
}
