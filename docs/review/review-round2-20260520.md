# Production Readiness Review (Round 2) — 2026-05-20

## Phạm vi
- Target: `jbe/AnNH/user-access-management/`
- Stack: Java 17, Spring Boot 3.4.4, Maven multi-module (`core`, `auth-service`, `user-service`), PostgreSQL, Kafka, Docker Compose
- Mục tiêu: đối chiếu source code hiện tại với các yêu cầu phải điều chỉnh trong `docs/review/` và đánh giá mức độ sẵn sàng production.

## Kết luận
Source code đã xử lý được nhiều điểm quan trọng từ Round 1, ví dụ:
- JWT secret đã chuyển sang đọc từ config/env.
- JWT invalid/expired đã được xử lý trả 401.
- Public register không còn cho client truyền role, luôn tạo `ROLE_USER`.
- DTO/controller đã bổ sung validation cơ bản.
- Dockerfile đã chuyển sang multi-stage và chạy non-root.
- Đã thêm actuator/prometheus.
- `mvn test` chạy thành công: tổng 64 tests pass.

Tuy nhiên, dự án **chưa đạt production-ready hoàn toàn**. Vẫn còn các blocker/rủi ro cần sửa trước khi deploy thật, đặc biệt là default runtime profile không an toàn và migration có thể xóa dữ liệu production.

## Findings cần điều chỉnh

### P1 — Default runtime vẫn rơi vào cấu hình dev không an toàn

**Vị trí:**
- `auth-service/src/main/resources/application.properties`
- `auth-service/src/main/resources/application-dev.properties`
- `user-service/src/main/resources/application.properties`
- `user-service/src/main/resources/application-dev.properties`

**Hiện trạng:**
- `application.properties` của cả 2 service đang set:

```properties
spring.profiles.active=dev
```

- Profile `dev` vẫn có fallback credential/secret:

```properties
spring.datasource.password=${DB_PASSWORD:123456}
jwt.secret=${JWT_SECRET:devSecretKeyForLocalDevelopmentOnly123456}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Rủi ro:**
- Nếu deploy thiếu biến môi trường hoặc quên set profile production/docker, app sẽ tự chạy bằng cấu hình dev.
- Có thể dùng password mặc định `123456`, JWT secret mặc định, tự update schema và log SQL.
- Không phù hợp tiêu chuẩn production vì fail-open thay vì fail-fast.

**Yêu cầu sửa:**
- Không set `spring.profiles.active=dev` trong `application.properties` dùng chung.
- Với production/docker, `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` phải bắt buộc có, không có fallback secret/password mặc định.
- Chỉ giữ fallback local ở profile dev nếu thật sự cần, và cần ghi rõ đây chỉ dùng local development.
- Production phải dùng `ddl-auto=validate` hoặc `none`, `show-sql=false`.

### P1 — Flyway migration của auth-service có migration phá dữ liệu

**Vị trí:**
- `auth-service/src/main/resources/db/migration/V2__refactor_users_table.sql`

**Hiện trạng:**
Migration đang drop các bảng hiện có:

```sql
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
```

Sau đó tạo lại bảng `users`.

**Rủi ro:**
- Nếu chạy trên database đã có dữ liệu, migration này sẽ xóa toàn bộ user.
- Đây là blocker production vì migration production phải bảo toàn dữ liệu hoặc có kế hoạch migration dữ liệu rõ ràng.

**Yêu cầu sửa:**
- Không dùng `DROP TABLE users` trong migration production.
- Viết migration dạng an toàn dữ liệu: `ALTER TABLE`, tạo cột mới, backfill dữ liệu, migrate role từ bảng cũ sang cột mới nếu cần.
- Nếu đây chỉ là database demo/local, tách rõ migration destructive ra khỏi production path.
- Cần test migration với database có dữ liệu mẫu trước khi chốt.

### P2 — Rate limit đã có nhưng chưa đủ production

**Vị trí:**
- `auth-service/src/main/java/com/r2s/auth/security/RateLimitFilter.java`

**Hiện trạng:**
- Rate limit dùng `ConcurrentHashMap<String, Bucket>` trong memory.
- Key là `request.getRemoteAddr()`.
- Không có cơ chế expire bucket theo IP.
- Không chia sẻ quota giữa nhiều instance.

**Rủi ro:**
- Khi scale nhiều instance, attacker có thể né limit bằng cách request qua instance khác.
- Khi chạy sau proxy/load balancer, `getRemoteAddr()` thường là IP của proxy chứ không phải client thật.
- Map có thể tăng dần theo số lượng IP và không được dọn.

**Yêu cầu sửa:**
- Với production, cân nhắc rate limit ở gateway/API gateway/ingress hoặc dùng storage chia sẻ như Redis.
- Nếu vẫn làm trong app, cần xử lý `X-Forwarded-For`/`Forwarded` một cách an toàn, có trusted proxy config.
- Thêm TTL/eviction cho bucket.
- Bổ sung test cho case vượt limit và case nhiều IP/proxy header nếu giữ filter trong app.

### P2 — Actuator/observability chưa nhất quán và có rủi ro expose metrics

**Vị trí:**
- `core/src/main/java/com/r2s/core/config/SecurityConstants.java`
- `auth-service/src/main/java/com/r2s/auth/config/SecurityConfig.java`
- `user-service/src/main/java/com/r2s/user/config/SecurityConfig.java`
- `auth-service/src/main/resources/application.properties`
- `user-service/src/main/resources/application.properties`
- `prometheus/prometheus.yml`

**Hiện trạng:**
- `SecurityConstants.PUBLIC_URLS` permit public cả:

```java
"/actuator/health",
"/actuator/prometheus"
```

- `auth-service` dùng `SecurityConstants.PUBLIC_URLS`, nên `/actuator/prometheus` public.
- `user-service` không dùng `SecurityConstants.PUBLIC_URLS`, chỉ permit `/users/public/**`, nên `/actuator/health` không public như báo cáo fix mô tả.

**Rủi ro:**
- Public `/actuator/prometheus` có thể lộ metadata vận hành nếu không được bảo vệ bởi network policy hoặc authentication riêng.
- Health check behavior giữa 2 service không nhất quán.
- Docker/Prometheus có thể hoạt động khác với kỳ vọng khi security config khác nhau.

**Yêu cầu sửa:**
- Thống nhất policy actuator cho cả 2 service.
- Chỉ public `/actuator/health` nếu cần cho health check.
- `/actuator/prometheus` nên được bảo vệ bằng network-level access, internal network, hoặc auth riêng.
- Cập nhật SecurityConfig của `user-service` để health check đúng kỳ vọng.
- Bổ sung test/security smoke test cho actuator endpoints.

### P3 — Build config còn warning chất lượng

**Vị trí:**
- `user-service/pom.xml`

**Hiện trạng:**
`user-service/pom.xml` khai báo trùng dependency:
- `org.springframework.security:spring-security-test`
- `com.h2database:h2`

Maven cảnh báo dependency declaration không unique và future Maven có thể không support model này.

**Rủi ro:**
- Build model thiếu sạch, dễ gây khác biệt dependency resolution về sau.
- Không đạt mức gọn gàng cần có cho production project.

**Yêu cầu sửa:**
- Xóa dependency khai báo trùng.
- Chạy lại `mvn test` để xác nhận build không còn warning duplicate dependency.

## Ghi chú kiểm chứng

Đã chạy:

```bash
mvn test
```

Kết quả:
- Reactor build thành công.
- `auth-service`: 38 tests pass.
- `user-service`: 26 tests pass.
- Tổng: 64 tests pass.

Các warning còn thấy trong quá trình test:
- `user-service/pom.xml` có duplicate dependencies.
- Một số test dùng `@MockBean` đã deprecated trong Spring Boot mới.
- Có warning Hibernate dialect khi test, cần rà lại cấu hình dialect/test profile nếu muốn build sạch hơn.

## Production gate

Trạng thái hiện tại: **chưa đạt production-ready**.

Ưu tiên sửa theo thứ tự:
1. Loại bỏ default runtime profile dev và fallback secret/password khỏi production path.
2. Sửa migration destructive trong `auth-service`.
3. Làm cứng rate limit cho môi trường scale/proxy.
4. Thống nhất và bảo vệ actuator endpoints.
5. Dọn Maven warnings và các deprecated test APIs.
