# Production Readiness Review (Round 1) — 2026-04-14

## Phạm vi
- Target: `jbe/AnNH/user-access-management/`
- Stack: Java 17, Spring Boot 3.4.4, Maven multi-module (`core`, `auth-service`, `user-service`), PostgreSQL (Docker Compose)

## Tóm tắt
Dự án chạy được theo hướng demo, nhưng **chưa production-ready** do có các vấn đề **bảo mật** (hardcoded secrets/credentials, role escalation) và **vận hành** (cấu hình JPA/log SQL). Các mục “Must-fix” bên dưới nên được xử lý trước khi triển khai thật.

## Must-fix (blocker trước khi lên production)

### 1) Hardcode JWT secret trong code
- Hiện tại `core` hardcode `SECRET_KEY` trong `JwtUtil`.
- Rủi ro: lộ secret → token bị giả mạo, khó rotate, không đáp ứng tiêu chuẩn bảo mật.
- Khuyến nghị:
  - Đọc secret từ env/config (ví dụ `JWT_SECRET`), không commit secret.
  - Thiết kế rotate secret (ít nhất hỗ trợ thay đổi secret không cần rebuild image).

### 2) JWT validation chưa đủ & dễ gây 500 thay vì 401
- `validateToken()` chỉ so `username`, không kiểm tra `exp`/claims; `extractUsername()` có thể ném `JwtException` khi token invalid/expired.
- Rủi ro: request có token lỗi/expired có thể dẫn đến 500 hoặc behavior không nhất quán.
- Khuyến nghị:
  - Bắt `JwtException`/`ExpiredJwtException` trong `JwtFilter`, trả **401** (không leak chi tiết).
  - Validate `exp` (và cân nhắc `issuer/audience` nếu cần).

### 3) Role escalation qua public register
- `RegisterRequest` cho phép client truyền `role`, và `AuthService.register()` set role theo request (mặc định `ROLE_USER` nếu null).
- Rủi ro: user tự đăng ký có thể lấy `ROLE_ADMIN`.
- Khuyến nghị:
  - Public register **không nhận role** (luôn `ROLE_USER`).
  - Tạo endpoint riêng để admin gán role (có `@PreAuthorize`).

### 4) Credentials DB hardcode trong `docker-compose` và `application.properties`
- `docker-compose.yaml` hardcode `POSTGRES_PASSWORD: 123456`.
- `application.properties` của cả 2 service hardcode `spring.datasource.password=123456`.
- Rủi ro: lộ mật khẩu, tái sử dụng credential, không phù hợp production.
- Khuyến nghị:
  - Dùng `.env` (không commit) hoặc secret manager (K8s Secret/ECS/Vault…).
  - Tách config theo environment; không để password thật trong repo.

### 5) `ddl-auto=update` và `show-sql=true` không phù hợp production
- `spring.jpa.hibernate.ddl-auto=update` và `spring.jpa.show-sql=true` xuất hiện ở config (kể cả profile docker).
- Rủi ro: schema bị tự động thay đổi ngoài kiểm soát, log SQL lộ dữ liệu/PII và tăng tải.
- Khuyến nghị:
  - Production: `ddl-auto=validate` hoặc `none`.
  - Dùng Flyway/Liquibase để quản lý migration.
  - Tắt `show-sql` và dùng logging có kiểm soát/masking.

## Should-fix (nâng chất lượng & vận hành)

### 1) Thiếu input validation ở controllers/DTOs
- Dự án có `spring-boot-starter-validation` (module `core`) nhưng DTO request chưa dùng `@NotBlank`, `@Size`, `@Email` và controller chưa dùng `@Valid`.
- Khuyến nghị:
  - Thêm constraint annotations cho `LoginRequest`, `RegisterRequest`, `UpdateUserRequest`.
  - Dùng `@Valid` trong controller, trả error response chuẩn hoá.

### 2) Error handling đang leak chi tiết nội bộ
- `GlobalExceptionHandler` trả `"Unexpected error: " + ex.getMessage()`.
- Rủi ro: lộ thông tin nhạy cảm (SQL, stack hints, message nội bộ).
- Khuyến nghị:
  - Chuẩn hoá response (error code, message tổng quát, timestamp, requestId).
  - Log chi tiết server-side, client nhận thông báo an toàn.

### 3) Thiếu hardening cho auth endpoints
- Chưa thấy rate limit/brute-force protection cho `/auth/login`.
- Khuyến nghị:
  - Thêm rate limiting (gateway/filter), hoặc lockout theo IP/user, tùy yêu cầu.

### 4) Docker image chưa tối ưu production
- Dockerfile dùng JDK runtime đầy đủ.
- Khuyến nghị:
  - Multi-stage build + runtime image nhẹ hơn, chạy non-root.
  - Cấu hình memory/GC qua `JAVA_TOOL_OPTIONS`.

### 5) Thiếu health checks & observability
- Chưa thấy actuator/health endpoints, metrics, tracing.
- Khuyến nghị:
  - Thêm `spring-boot-starter-actuator` và bật readiness/liveness.
  - Structured logging + correlation id; OpenTelemetry nếu cần trace.

## Kết luận (production gate)
Trạng thái hiện tại: **chưa đạt production-ready**.

Ưu tiên thực hiện theo thứ tự:
1) Secrets/credentials (JWT + DB) và role escalation
2) JWT validation & trả lỗi 401/403 đúng chuẩn
3) Tách cấu hình environment + bỏ `ddl-auto=update`/`show-sql`
4) Validation, error response chuẩn hoá, observability/actuator

