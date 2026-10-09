package com.bemock.prep.dto;

import com.bemock.prep.model.Role;
import com.bemock.prep.model.User;

import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
