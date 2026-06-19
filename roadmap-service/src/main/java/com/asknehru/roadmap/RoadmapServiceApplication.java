package com.asknehru.roadmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.asknehru.roadmap",
        "com.asknehru.auth.config",
        "com.asknehru.auth.security",
        "com.asknehru.auth.repository",
        "com.asknehru.auth.service"
})
@EntityScan(basePackages = {
        "com.asknehru.roadmap.model",
        "com.asknehru.auth.model"
})
@EnableJpaRepositories(basePackages = {
        "com.asknehru.roadmap.repository",
        "com.asknehru.auth.repository"
})
public class RoadmapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoadmapServiceApplication.class, args);
    }
}
