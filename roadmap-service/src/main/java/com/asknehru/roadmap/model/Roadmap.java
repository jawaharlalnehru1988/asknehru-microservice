package com.asknehru.roadmap.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roadmaps")
@Getter
@Setter
@NoArgsConstructor
public class Roadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column
    private String intro;

    @Column(name = "main_topic", nullable = false)
    private String mainTopic;

    @Column(name = "router_link")
    private String routerLink;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadmapChapter> chapters = new ArrayList<>();

    @Column(name = "user_assigned_roadmap")
    private Boolean userAssignedRoadmap = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
