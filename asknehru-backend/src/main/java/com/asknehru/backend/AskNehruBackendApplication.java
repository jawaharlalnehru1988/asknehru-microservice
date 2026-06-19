package com.asknehru.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.asknehru")
@EntityScan(basePackages = "com.asknehru")
@EnableJpaRepositories(basePackages = "com.asknehru")
public class AskNehruBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AskNehruBackendApplication.class, args);
    }
}
