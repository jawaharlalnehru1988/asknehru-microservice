package com.asknehru.user.api;

import com.asknehru.user.model.DjangoUser;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(
            Integer id,
            String username,
            String email,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static UserResponse fromEntity(DjangoUser user) {
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDateJoined(),
                    user.getLastLogin()
            );
        }
    }

    public record CreateUserRequest(
            @Size(min = 3, max = 50) String username,
            @Email @Size(max = 254) String email,
            @Size(min = 6, max = 72) String password
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UpdateUserRequest(
            @Size(min = 3, max = 50) String username,
            @Email @Size(max = 254) String email,
            @Size(min = 6, max = 72) String password
    ) {
    }
}
