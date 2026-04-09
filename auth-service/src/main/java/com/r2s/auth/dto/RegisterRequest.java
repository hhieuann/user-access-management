package com.r2s.auth.dto;

import com.r2s.auth.entity.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private Role role;
}