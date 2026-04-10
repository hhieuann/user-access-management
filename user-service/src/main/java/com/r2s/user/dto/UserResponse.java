package com.r2s.user.dto;

import com.r2s.user.entity.User;

public class UserResponse {
    private String username;
    private String role;
    private String email;
    private String fullName;

    public static UserResponse fromEntity(User user) {
        UserResponse res = new UserResponse();
        res.setUsername(user.getUsername());
        res.setRole(user.getRole().name());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        return res;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}