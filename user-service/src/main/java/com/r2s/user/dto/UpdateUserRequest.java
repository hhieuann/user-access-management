package com.r2s.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 100)
    private String fullName;

    @Email(message = "Email format is invalid")
    @Size(max = 100)
    private String email;
}