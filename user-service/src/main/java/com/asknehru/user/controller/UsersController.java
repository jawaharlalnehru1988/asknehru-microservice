package com.asknehru.user.controller;

import com.asknehru.user.api.UserDtos.CreateUserRequest;
import com.asknehru.user.api.UserDtos.UpdateUserRequest;
import com.asknehru.user.api.UserDtos.UserResponse;
import com.asknehru.user.model.DjangoUser;
import com.asknehru.user.repository.DjangoUserRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final DjangoUserRepository djangoUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UsersController(DjangoUserRepository djangoUserRepository, PasswordEncoder passwordEncoder) {
        this.djangoUserRepository = djangoUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return djangoUserRepository.findAllByOrderByIdAsc().stream().map(UserResponse::fromEntity).toList();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        String normalizedUsername = normalize(request.username());
        String normalizedEmail = normalizeEmail(request.email());

        if (djangoUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists."));
        }
        if (djangoUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists."));
        }

        DjangoUser user = new DjangoUser();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDateJoined(Instant.now());

        DjangoUser created = djangoUserRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Integer id) {
        return UserResponse.fromEntity(getUserOr404(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @Valid @RequestBody UpdateUserRequest request) {
        DjangoUser user = getUserOr404(id);

        if (request.username() != null) {
            String normalizedUsername = normalize(request.username());
            if (djangoUserRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username already exists."));
            }
            user.setUsername(normalizedUsername);
        }

        if (request.email() != null) {
            String normalizedEmail = normalizeEmail(request.email());
            if (djangoUserRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already exists."));
            }
            user.setEmail(normalizedEmail);
        }

        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        DjangoUser updated = djangoUserRepository.save(user);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        DjangoUser user = getUserOr404(id);
        djangoUserRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private DjangoUser getUserOr404(Integer id) {
        return djangoUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }
}
