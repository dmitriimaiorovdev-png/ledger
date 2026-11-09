package com.example.ledger.dto;

import com.example.ledger.model.User;
import java.time.Instant;

public record UserDto(Long id, String email, String firstName, String lastName,
                      String role, String status, Instant createdAt) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? null : user.getStatus().name(), user.getCreatedAt());
    }
}
