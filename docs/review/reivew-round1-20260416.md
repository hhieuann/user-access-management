## Đối chiếu theo từng tiêu chí Round 1

## Must-fix
- **1) Hardcode JWT secret trong code**: **ĐÃ FIX**
  - `core/src/main/java/com/r2s/core/security/JwtUtil.java` dùng `@Value("${jwt.secret}")` thay vì hardcode.
  - `*-service/src/main/resources/application-docker.properties` map `jwt.secret=${JWT_SECRET}`.

- **2) JWT validation chưa đủ & dễ gây 500 thay vì 401**: **ĐÃ FIX (cơ bản)**
  - `JwtUtil.validateToken()` có check `exp` và bắt `JwtException`.
  - `auth-service/src/main/java/com/r2s/auth/security/JwtFilter.java` và `user-service/.../JwtFilter.java` bắt `JwtException` khi parse token và trả **401**.

- **3) Role escalation qua public register**: **ĐÃ FIX**
  - `auth-service/src/main/java/com/r2s/auth/dto/RegisterRequest.java` **không còn field `role`**.
  - `auth-service/src/main/java/com/r2s/auth/service/AuthService.java` public register **luôn set** `Role.ROLE_USER`.
  - Có endpoint assign role cho admin: `AuthController` dùng `@PreAuthorize("hasRole('ADMIN')")`.

- **4) Credentials DB hardcode trong docker-compose và application.properties**: **CHƯA ĐẠT (mới fix 1 phần)**
  - `docker-compose.yaml`: **ĐÃ FIX** (dùng `${POSTGRES_PASSWORD}`).
  - Nhưng `auth-service/src/main/resources/application.properties` và `user-service/.../application.properties` vẫn còn `spring.datasource.password=123456`.
  - `application-dev.properties` và `application-test.properties` cũng còn `123456`.

- **5) `ddl-auto=update` và `show-sql=true` không phù hợp production**: **CHƯA ĐẠT (mới fix cho profile docker)**
  - `*-service/src/main/resources/application-docker.properties`: **ĐÃ FIX** (`ddl-auto=validate`, `show-sql=false`).
  - Nhưng `application.properties` và `application-dev.properties` vẫn để `ddl-auto=update`, `show-sql=true` (và `test` còn `show-sql=true`).

## Should-fix
- **1) Thiếu input validation ở controllers/DTOs**: **ĐÃ FIX một phần**
  - `LoginRequest`, `RegisterRequest`, `UpdateUserRequest` đã có annotation validation.
  - `AuthController` đã dùng `@Valid`.
  - Nhưng `user-service/src/main/java/com/r2s/user/controller/UserController.java` **chưa dùng `@Valid`** cho `UpdateUserRequest` ở `PUT /users/me`, nên validation có thể không chạy.

- **2) Error handling leak chi tiết**: **ĐÃ FIX**
  - `core/src/main/java/com/r2s/core/exception/GlobalExceptionHandler.java` trả message tổng quát cho lỗi 500 và có handler cho validation.

- **3) Rate limit/brute-force protection**: **CHƯA THẤY**
  - Không thấy cấu hình/thư viện rate limit (bucket4j/resilience4j…) hay cơ chế lockout cho `/auth/login`.

- **4) Docker image tối ưu production**: **CHƯA ĐẠT**
  - `auth-service/Dockerfile` và `user-service/Dockerfile` vẫn dùng base `eclipse-temurin:17-jdk-jammy`, **chưa** multi-stage, **chưa** non-root runtime.

- **5) Health checks & observability (actuator/metrics/tracing)**: **CHƯA THẤY**
  - Không thấy dependency `spring-boot-starter-actuator` (search chỉ ra nó mới xuất hiện trong chính file review).
