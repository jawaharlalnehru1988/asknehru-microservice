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
            String fullName,
            String role,
            boolean isStaff,
            boolean isSuperuser,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static UserResponse fromEntity(DjangoUser user) {
            boolean isAdmin = user.isSuperuser() || user.isStaff()
                    || "nehru00123@gmail.com".equalsIgnoreCase(user.getEmail())
                    || "jawaharlalnehruelumalai@gmail.com".equalsIgnoreCase(user.getEmail());
            String fullName = user.getFirstName() != null && !user.getFirstName().isBlank()
                    ? user.getFirstName()
                    : user.getUsername();
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    fullName,
                    isAdmin ? "Super Admin" : "User",
                    user.isStaff(),
                    user.isSuperuser(),
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
