package com.asknehru.user.listener;

import com.asknehru.contracts.events.UserRegisteredEvent;
import com.asknehru.user.model.DjangoUser;
import com.asknehru.user.repository.DjangoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationListener.class);
    private final DjangoUserRepository djangoUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationListener(DjangoUserRepository djangoUserRepository, PasswordEncoder passwordEncoder) {
        this.djangoUserRepository = djangoUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @KafkaListener(topics = "user-registration-topic", groupId = "user-service-group")
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {}", event.getEmail());

        if (djangoUserRepository.existsByEmailIgnoreCase(event.getEmail())) {
            log.info("User with email {} already exists in DjangoUser table.", event.getEmail());
            return;
        }

        DjangoUser user = new DjangoUser();
        // Use email as username since auth-service relies heavily on email
        user.setUsername(event.getEmail());
        user.setEmail(event.getEmail());
        // Set a random impossible password since auth is handled by auth-service
        user.setPassword(passwordEncoder.encode("KAFKA_SYNCED_" + event.getUserId()));
        user.setDateJoined(Instant.now());

        djangoUserRepository.save(user);
        log.info("Successfully synchronized user profile for {}", event.getEmail());
    }
}
