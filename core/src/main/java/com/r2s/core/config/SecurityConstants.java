package com.r2s.core.config;

public final class SecurityConstants {

    private SecurityConstants() {} // Không cho tạo instance

    // JWT
    public static final long JWT_EXPIRATION_MS = 86_400_000L; // 1 ngày
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Endpoints công khai
    public static final String[] PUBLIC_URLS = {
            "/auth/register",
            "/auth/login",
            "/actuator/health",
            "/actuator/prometheus"  // ← Thêm
    };

    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MODERATOR = "MODERATOR";
}