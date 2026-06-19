package com.asknehru.contracts.events;

public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String fullName;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(Long userId, String email, String fullName) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
