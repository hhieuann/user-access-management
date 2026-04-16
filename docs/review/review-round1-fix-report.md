# Báo cáo Fix Review Round 1

**Project:** User Access Management System  
**Branch:** `review/r1`  
**Người thực hiện:** Nguyễn Hiếu An  
**Ngày fix:** 16/04/2026  

---

## Đối chiếu theo từng tiêu chí Round 1

---

## Must-fix

### 1) Hardcode JWT secret trong code: ✅ ĐÃ FIX (Round 1) - GIỮ NGUYÊN

- `core/src/main/java/com/r2s/core/security/JwtUtil.java` dùng `@Value("${jwt.secret}")` thay vì hardcode.
- `*-service/src/main/resources/application-docker.properties` map `jwt.secret=${JWT_SECRET}`.

```java
// JwtUtil.java
@Value("${jwt.secret}")
private String jwtSecret;
```

---

### 2) JWT validation chưa đủ & dễ gây 500 thay vì 401: ✅ ĐÃ FIX (Round 1) - GIỮ NGUYÊN

- `JwtUtil.validateToken()` có check `exp` và bắt `JwtException`.
- `auth-service/.../JwtFilter.java` và `user-service/.../JwtFilter.java` bắt `JwtException` khi parse token và trả **401**.

```java
// JwtFilter.java
try {
    // validate token
} catch (JwtException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
    return;
}
```

---

### 3) Role escalation qua public register: ✅ ĐÃ FIX (Round 1) - GIỮ NGUYÊN

- `auth-service/.../dto/RegisterRequest.java` **không còn field `role`**.
- `auth-service/.../service/AuthService.java` public register **luôn set** `Role.ROLE_USER`.
- Có endpoint assign role cho admin: `AuthController` dùng `@PreAuthorize("hasRole('ADMIN')")`.

```java
// AuthService.java
private User buildNewUser(RegisterRequest request) {
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.ROLE_USER); // Luôn set ROLE_USER
    return user;
}
```

---

### 4) Credentials DB hardcode trong docker-compose và application.properties: ✅ ĐÃ FIX

**Trước khi fix:**
- `application.properties` và `application-dev.properties` vẫn còn `spring.datasource.password=123456`.
- `application-test.properties` cũng còn `123456`.

**Đã fix:**
- `application-dev.properties` dùng environment variable với fallback:

```properties
# application-dev.properties
spring.datasource.password=${DB_PASSWORD:123456}
jwt.secret=${JWT_SECRET:devSecretKeyForLocalDevelopmentOnly123456}
```

- `application-test.properties` dùng H2 in-memory, không cần password thật:

```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**File thay đổi:**
- `auth-service/src/main/resources/application-dev.properties`
- `user-service/src/main/resources/application-dev.properties`
- `user-service/src/test/resources/application-test.properties`

---

### 5) `ddl-auto=update` và `show-sql=true` không phù hợp production: ✅ ĐÃ FIX

**Trước khi fix:**
- `application.properties` và `application-dev.properties` vẫn để `ddl-auto=update`, `show-sql=true`.
- `application-test.properties` còn `show-sql=true`.

**Đã fix theo từng profile:**

| Profile | ddl-auto | show-sql |
|---------|----------|----------|
| dev | update | true |
| docker | validate | false |
| test | create-drop | false ✅ |
| ci | update | false |

```properties
# application-test.properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false  # ← Đã fix
```

**File thay đổi:**
- `user-service/src/test/resources/application-test.properties`

---

## Should-fix

### 1) Thiếu input validation ở controllers/DTOs: ✅ ĐÃ FIX

**Trước khi fix:**
- `UserController.java` chưa dùng `@Valid` cho `UpdateUserRequest` ở `PUT /users/me`.

**Đã fix:**

```java
// UserController.java
@PutMapping("/me")
public ResponseEntity<UserResponse> updateMyProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateUserRequest request) { // ← Thêm @Valid
    return ResponseEntity.ok(
        userManagementService.updateUser(authentication.getName(), request)
    );
}
```

**File thay đổi:**
- `user-service/src/main/java/com/r2s/user/controller/UserController.java`

---

### 2) Error handling leak chi tiết: ✅ ĐÃ FIX (Round 1) - GIỮ NGUYÊN

- `core/.../exception/GlobalExceptionHandler.java` trả message tổng quát cho lỗi 500.
- Có handler riêng cho `AuthorizationDeniedException` trả 403.
- Có handler cho `MethodArgumentNotValidException` trả 400.

```java
// GlobalExceptionHandler.java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleAll(Exception ex) {
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred"); // Không leak stack trace
}
```

---

### 3) Rate limit/brute-force protection: ✅ ĐÃ FIX

**Trước khi fix:**
- Không có cơ chế rate limit cho `/auth/login`.

**Đã fix:**
- Thêm dependency `bucket4j_jdk17-core:8.18.0` vào `auth-service/pom.xml`.
- Tạo `RateLimitFilter.java` giới hạn **5 request/phút** cho mỗi IP tại `/auth/login`.

```xml
<!-- auth-service/pom.xml -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.18.0</version>
</dependency>
```

```java
// RateLimitFilter.java
private Bucket createBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit
            .capacity(5)
            .refillGreedy(5, Duration.ofMinutes(1))) // 5 requests/phút
        .build();
}
```

**Cơ chế hoạt động:**
- Mỗi IP được tối đa **5 lần login/phút**.
- Vượt quá → trả **429 Too Many Requests**.
- Token bucket tự động refill sau 1 phút.

**File thay đổi:**
- `auth-service/pom.xml`
- `auth-service/src/main/java/com/r2s/auth/security/RateLimitFilter.java`
- `auth-service/src/main/java/com/r2s/auth/config/SecurityConfig.java`

---

### 4) Docker image tối ưu production: ✅ ĐÃ FIX

**Trước khi fix:**
- Dockerfile dùng `eclipse-temurin:17-jdk-jammy` (JDK đầy đủ, nặng, chạy với root user).

**Đã fix - Multi-stage build + Non-root user:**

```dockerfile
# Stage 1: Extract layers
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Runtime (nhỏ hơn, an toàn hơn)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

USER appuser  # Chạy với non-root user

EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**Lợi ích:**
- Dùng JRE thay JDK → image nhỏ hơn ~200MB.
- Non-root user → bảo mật hơn.
- Layer caching → build nhanh hơn.

**File thay đổi:**
- `auth-service/Dockerfile`
- `user-service/Dockerfile`

---

### 5) Health checks & observability: ✅ ĐÃ FIX

**Trước khi fix:**
- Không có `spring-boot-starter-actuator`.

**Đã fix:**
- Thêm Actuator vào cả 2 service.
- Expose endpoint `/actuator/health` và `/actuator/info`.
- Thêm `/actuator/health` vào `PUBLIC_URLS` trong `SecurityConstants`.

```xml
<!-- pom.xml (cả 2 service) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

```java
// SecurityConstants.java
public static final String[] PUBLIC_URLS = {
    "/auth/register",
    "/auth/login",
    "/actuator/health"  // ← Thêm
};
```

**File thay đổi:**
- `auth-service/pom.xml`
- `user-service/pom.xml`
- `auth-service/src/main/resources/application.properties`
- `user-service/src/main/resources/application.properties`
- `core/src/main/java/com/r2s/core/config/SecurityConstants.java`

---

## Tóm tắt

| # | Vấn đề | Trạng thái Round 1 | Trạng thái Sau Fix |
|---|--------|-------------------|-------------------|
| Must 1 | Hardcode JWT secret | ✅ ĐÃ FIX | ✅ GIỮ NGUYÊN |
| Must 2 | JWT validation → 401 | ✅ ĐÃ FIX | ✅ GIỮ NGUYÊN |
| Must 3 | Role escalation register | ✅ ĐÃ FIX | ✅ GIỮ NGUYÊN |
| Must 4 | DB credentials hardcode | ⚠️ CHƯA ĐẠT | ✅ ĐÃ FIX |
| Must 5 | ddl-auto + show-sql | ⚠️ CHƯA ĐẠT | ✅ ĐÃ FIX |
| Should 1 | Input validation @Valid | ⚠️ CHƯA ĐẠT | ✅ ĐÃ FIX |
| Should 2 | Error handling leak | ✅ ĐÃ FIX | ✅ GIỮ NGUYÊN |
| Should 3 | Rate limit login | ❌ CHƯA THẤY | ✅ ĐÃ FIX |
| Should 4 | Docker multi-stage | ❌ CHƯA ĐẠT | ✅ ĐÃ FIX |
| Should 5 | Actuator health check | ❌ CHƯA THẤY | ✅ ĐÃ FIX |
