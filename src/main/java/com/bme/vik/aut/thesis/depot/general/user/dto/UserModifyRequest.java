package com.bme.vik.aut.thesis.depot.general.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class UserModifyRequest {
    private String userName;
    private String password;
}
