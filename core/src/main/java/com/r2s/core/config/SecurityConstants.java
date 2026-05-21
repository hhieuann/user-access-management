package com.r2s.core.config;

public final class SecurityConstants {

    private SecurityConstants() {} // Không cho tạo instance

    // JWT
    public static final long JWT_EXPIRATION_MS = 86_400_000L; // 1 ngày
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Endpoints công khai - dung chung cho ca 2 service.
    //
    // LUU Y PRODUCTION:
    // - /actuator/health public de health check (k8s/load balancer) OK.
    // - /actuator/prometheus dang public de Prometheus scrape qua docker network.
    //   Production goi y:
    //     + Dat actuator len management.server.port rieng (vd 9081/9082),
    //       chi expose port nay trong internal network.
    //     + Hoac dung basic-auth o Spring Security cho /actuator/prometheus.
    //     + Hoac IP allowlist o reverse proxy/ingress.
    public static final String[] PUBLIC_URLS = {
            "/auth/register",
            "/auth/login",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    };

    // Actuator URLs - dung rieng khi can phan biet voi business URLs.
    public static final String[] ACTUATOR_PUBLIC_URLS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    };

    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MODERATOR = "MODERATOR";
}