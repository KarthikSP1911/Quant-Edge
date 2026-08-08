package com.quantedge.backend.dto.auth;

import java.util.UUID;

import com.quantedge.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthUserResponse {
    private UUID id;
    private String email;
    private String name;
    private Role role;
}
