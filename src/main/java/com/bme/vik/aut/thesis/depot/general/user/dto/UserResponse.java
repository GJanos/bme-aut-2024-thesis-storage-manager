package com.bme.vik.aut.thesis.depot.general.user.dto;

import com.bme.vik.aut.thesis.depot.security.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Integer id;
    private String userName;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}