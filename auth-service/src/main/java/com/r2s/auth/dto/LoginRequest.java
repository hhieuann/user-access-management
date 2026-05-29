package com.r2s.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Loai xac thuc (cho Strategy Pattern).
     * Default = null -> coi nhu "password" (backward compatible).
     * Cac value khac trong tuong lai: "google-oauth", "saml", "2fa"...
     */
    private String authType;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
}