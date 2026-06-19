package com.asknehru.knowledgebase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.asknehru.knowledgebase",
        "com.asknehru.auth.config",
        "com.asknehru.auth.security",
        "com.asknehru.auth.repository",
        "com.asknehru.auth.service"
})
@EntityScan(basePackages = {
        "com.asknehru.knowledgebase.model",
        "com.asknehru.auth.model"
})
@EnableJpaRepositories(basePackages = {
        "com.asknehru.knowledgebase.repository",
        "com.asknehru.auth.repository"
})
public class KnowledgeBaseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseServiceApplication.class, args);
    }
}
