package com.asknehru.roadmap.listener;

import com.asknehru.contracts.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("roadmapUserRegistrationListener")
public class UserRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationListener.class);

    @KafkaListener(topics = "user-registration-topic", groupId = "roadmap-service-group")
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {}. Preparing to initialize default roadmap.", event.getEmail());
        // In the future, call a roadmapService.createDefaultRoadmap(event.getUserId());
    }
}
