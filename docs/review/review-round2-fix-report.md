# Báo cáo Fix Production Readiness Review (Round 2)

**Project:** User Access Management System
**Người thực hiện:** Nguyễn Hiếu An
**Ngày fix:** 20/05/2026
**Review file gốc:** Production Readiness Review (Round 2) — 2026-05-20

---

## Tóm tắt

| Finding | Mức độ | Status | Số file đụng đến |
|---|---|---|---|
| P1.1 — Default runtime profile dev không an toàn | Blocker | ✅ FIXED | 7 |
| P1.2 — Flyway V2 destructive (`DROP TABLE`) | Blocker | ✅ FIXED | 1 |
| P2.1 — Rate limit chưa đủ production | Medium | ✅ FIXED | 1 + 1 test mới |
| P2.2 — Actuator policy không nhất quán giữa 2 service | Medium | ✅ FIXED | 2 + 2 tests mới |
| P3 — Duplicate dependencies trong `user-service/pom.xml` | Low | ✅ FIXED | 1 |

**Kết quả test sau fix:**
- auth-service: 44/44 PASS (tăng từ 36 → 44 do thêm 6 RateLimit tests + 2 Actuator smoke tests)
- user-service: 28/28 PASS (tăng từ 26 → 28 do thêm 2 Actuator smoke tests)
- Tổng: **72/72 automated tests pass** (chưa tính 2 E2E EmbeddedKafka — vấn đề môi trường Windows đã ghi nhận trong Testing_Guide)

---

## P1.1 — Default runtime profile dev không an toàn

### Trước khi sửa (vi phạm)

`auth-service/src/main/resources/application.properties` và `user-service/src/main/resources/application.properties`:
```properties
spring.profiles.active=dev    # ❌ Default rơi vào dev
```

`application-dev.properties`:
```properties
spring.datasource.password=${DB_PASSWORD:123456}                        # ❌ Fallback weak password
jwt.secret=${JWT_SECRET:devSecretKeyForLocalDevelopmentOnly123456}      # ❌ Fallback weak secret
spring.jpa.hibernate.ddl-auto=update                                    # ❌ Hibernate tự sửa schema
spring.jpa.show-sql=true                                                # ❌ Lộ SQL trong log
```

→ Nếu deploy production mà quên set `SPRING_PROFILES_ACTIVE`, app **tự chạy dev profile** với credential mặc định, JWT secret yếu, và Hibernate tự update schema → fail-open.

### Sau khi sửa

**1. `application.properties` (cả 2 service):** xoá hẳn `spring.profiles.active=dev`
```properties
server.port=8081
spring.application.name=auth-service
# Khong set spring.profiles.active mac dinh.
# Phai truyen profile qua SPRING_PROFILES_ACTIVE env hoac -Dspring.profiles.active=...
# Cac profile co san: dev | docker | test | ci | prod
```
→ Nếu deploy thiếu env, Spring Boot fail-fast vì không tìm thấy `${SPRING_DATASOURCE_URL}` (không có fallback). Đáp ứng nguyên tắc fail-fast.

**2. `application-dev.properties` (cả 2 service):** giữ fallback (để IDE chạy được local) nhưng:
- Thêm header rõ ràng "CHI DUNG CHO LOCAL DEVELOPMENT"
- Đổi `ddl-auto=update` → `ddl-auto=none` (đã có Flyway quản schema)

```properties
# =========================================================
# DEV PROFILE - CHI DUNG CHO LOCAL DEVELOPMENT
# =========================================================
# Khong duoc set profile nay khi deploy production.
# Production phai dung profile 'docker' hoac 'prod'.
# =========================================================
spring.datasource.password=${DB_PASSWORD:123456}        # Local-only fallback
spring.jpa.hibernate.ddl-auto=none                       # ✅ Flyway lam chu schema
```

**3. Tạo mới `application-prod.properties` (cả 2 service):** profile production thuần fail-fast, không fallback
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}              # Không có default
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}    # Không có default
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}    # Không có default
spring.jpa.hibernate.ddl-auto=validate                       # ✅ Production-safe
spring.jpa.show-sql=false                                    # ✅ Không log SQL
jwt.secret=${JWT_SECRET}                                     # Không có default
spring.flyway.baseline-on-migrate=false                      # Production phải migration sạch
```

**4. `application-ci.properties` (cả 2 service):** đổi `ddl-auto=update` → `ddl-auto=none` + bật Flyway tường minh

**Files đã sửa:**
- `auth-service/src/main/resources/application.properties`
- `auth-service/src/main/resources/application-dev.properties`
- `auth-service/src/main/resources/application-ci.properties`
- `auth-service/src/main/resources/application-prod.properties` (mới)
- `user-service/src/main/resources/application.properties`
- `user-service/src/main/resources/application-dev.properties`
- `user-service/src/main/resources/application-ci.properties`
- `user-service/src/main/resources/application-prod.properties` (mới)

---

## P1.2 — Flyway V2 destructive

### Trước khi sửa (vi phạm)

`auth-service/src/main/resources/db/migration/V2__refactor_users_table.sql`:
```sql
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;     -- ❌ XOÁ TOÀN BỘ USER!

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    ...
);
```
→ Nếu chạy trên DB production có dữ liệu, **mất sạch user**. Blocker tuyệt đối.

### Sau khi sửa

Migration mới thực hiện theo nguyên tắc *preserve user data*:

```sql
-- Buoc 1: Them cot 'role' (khong drop bang)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);

-- Buoc 2: Backfill role tu bang M2M cu (user_roles + roles)
DO $$
BEGIN
    IF EXISTS (...) THEN
        UPDATE users u SET role = sub.role_name
        FROM (
            SELECT DISTINCT ON (ur.user_id) ur.user_id, r.name AS role_name
            FROM user_roles ur JOIN roles r ON r.id = ur.role_id
            ORDER BY ur.user_id, r.id
        ) sub WHERE sub.user_id::text = u.id::text AND u.role IS NULL;
    END IF;
END $$;

UPDATE users SET role = 'ROLE_USER' WHERE role IS NULL;
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- Buoc 3: Drop bang trung gian (data da backfill xong)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;

-- Buoc 4: Drop cot khong dung
ALTER TABLE users DROP COLUMN IF EXISTS enabled;

-- Buoc 5: Chuyen kieu users.id UUID -> BIGINT (preserve ban ghi)
DO $$
BEGIN
    -- Them id_new BIGSERIAL, drop PK cu, drop cot UUID, rename
    -- Cac ban ghi users van giu username/password/role
END $$;
```

**Đảm bảo an toàn:**
1. KHÔNG có `DROP TABLE users` ở bất kỳ chỗ nào.
2. Backfill `role` từ bảng M2M trước khi drop M2M → không mất role gốc.
3. `ALTER TABLE` thay vì recreate → username/password/role được giữ nguyên.
4. Có guard `IF EXISTS` để migration idempotent với DB đã được Hibernate auto-update sẵn.

**Cảnh báo còn lại (đã ghi rõ trong comment migration):**
- Trước khi chạy trên DB production có dữ liệu thật, **phải backup**.
- Bước 5 (đổi UUID → BIGINT) là thao tác lớn, cần kiểm thử với DB sample trước.

**File đã sửa:**
- `auth-service/src/main/resources/db/migration/V2__refactor_users_table.sql`

---

## P2.1 — Rate limit hardening

### Trước khi sửa (vi phạm)

`auth-service/src/main/java/com/r2s/auth/security/RateLimitFilter.java`:
```java
private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();   // ❌ Không TTL
String ip = request.getRemoteAddr();                                      // ❌ Sau proxy = IP proxy
```

Vấn đề:
1. Map grow vô hạn theo số IP từng request.
2. Sau reverse proxy/load balancer, `getRemoteAddr()` trả IP proxy → mọi client chia chung 1 bucket → rate limit hoạt động sai.
3. Không có test case.

### Sau khi sửa

**1. TTL eviction:**
```java
record BucketEntry(Bucket bucket, AtomicLong lastAccess) {...}
private static final Duration BUCKET_TTL = Duration.ofMinutes(10);

// Background thread: don bucket cu moi phut
cleanupExecutor.scheduleAtFixedRate(this::evictExpiredBuckets, 1, 1, TimeUnit.MINUTES);

void evictExpiredBuckets() {
    Instant cutoff = Instant.now().minus(BUCKET_TTL);
    buckets.entrySet().removeIf(e -> e.getValue().lastAccess().isBefore(cutoff));
}
```

**2. X-Forwarded-For an toàn (chỉ tin khi request đến từ trusted proxy):**
```java
@Value("${app.rate-limit.trusted-proxies:}") String trustedProxiesCsv

String resolveClientIp(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    if (trustedProxies.isEmpty() || !trustedProxies.contains(remoteAddr)) {
        return remoteAddr;   // Không phải proxy → KHÔNG đọc XFF (chống fake header)
    }
    String xff = request.getHeader("X-Forwarded-For");
    // Lấy phần tử đầu tiên (client thật)
    int comma = xff.indexOf(',');
    return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
}
```

Cấu hình production:
```properties
# Khi deploy sau nginx/ingress
app.rate-limit.trusted-proxies=10.0.0.1,10.0.0.2
```

**3. Comment cảnh báo production:**
```java
/**
 * LUU Y PRODUCTION:
 *   - Filter nay luu bucket trong memory cua moi instance.
 *     Khi scale > 1 instance, attacker co the ne limit qua instance khac.
 *     Production goi y dat rate limit o API gateway/ingress, hoac dung Redis chia se.
 */
```

**4. Tests mới (6 test cases):**

| ID | Test | Mục đích |
|---|---|---|
| TC101 | `doFilter_NonLoginEndpoint_PassesThrough` | Endpoint khác /auth/login không bị limit |
| TC102 | `doFilter_OverLimit_Returns429` | Lần 6 trả 429 |
| TC103 | `doFilter_DifferentIps_HaveIndependentBuckets` | 2 IP có quota riêng |
| TC104 | `resolveClientIp_TrustedProxy_ReadsXForwardedFor` | Tin XFF khi đến từ trusted proxy |
| TC105 | `resolveClientIp_UntrustedRemote_IgnoresXForwardedFor` | Ignore XFF khi không phải trusted proxy (chống fake header) |
| TC106 | `evictExpiredBuckets_RemovesStaleEntries` | Bucket cũ qua TTL bị xoá |

**Files đã sửa/tạo:**
- `auth-service/src/main/java/com/r2s/auth/security/RateLimitFilter.java`
- `auth-service/src/test/java/com/r2s/auth/security/RateLimitFilterTest.java` (mới)

---

## P2.2 — Thống nhất Actuator policy

### Trước khi sửa (vi phạm)

- `auth-service/SecurityConfig` → public `/actuator/health`, `/actuator/prometheus` qua `SecurityConstants.PUBLIC_URLS` ✓
- `user-service/SecurityConfig` → **chỉ** permit `/users/public/**`, không có `/actuator/**` ❌
  → Prometheus container không scrape được user-service vì trả 401/403.

### Sau khi sửa

**1. Thêm `ACTUATOR_PUBLIC_URLS` riêng trong `SecurityConstants.java`:**
```java
// Actuator URLs - dung rieng khi can phan biet voi business URLs.
public static final String[] ACTUATOR_PUBLIC_URLS = {
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus"
};
```

**2. `user-service/SecurityConfig.java` dùng cùng constants:**
```java
import com.r2s.core.config.SecurityConstants;
...
.authorizeHttpRequests(auth -> auth
        // Actuator public (thong nhat voi auth-service qua SecurityConstants).
        .requestMatchers(SecurityConstants.ACTUATOR_PUBLIC_URLS).permitAll()
        .requestMatchers("/users/public/**").permitAll()
        .anyRequest().authenticated()
)
```

**3. Cảnh báo production trong `SecurityConstants.java`:**
```java
// LUU Y PRODUCTION:
// - /actuator/health public de health check (k8s/load balancer) OK.
// - /actuator/prometheus dang public de Prometheus scrape qua docker network.
//   Production goi y:
//     + Dat actuator len management.server.port rieng (vd 9081/9082),
//       chi expose port nay trong internal network.
//     + Hoac dung basic-auth o Spring Security cho /actuator/prometheus.
//     + Hoac IP allowlist o reverse proxy/ingress.
```

**4. `application.properties` (cả 2): siết exposure list (bỏ `metrics`, đổi health show-details=never):**
```properties
# Truoc: include=health,info,prometheus,metrics  + show-details=when-authorized
# Sau:   include=health,info,prometheus           + show-details=never
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=never
```
→ Giảm tối thiểu thông tin lộ ra ngoài.

**5. Smoke tests cho cả 2 service (4 test cases mới):**

| ID | Test | Service |
|---|---|---|
| TC107 | `actuatorHealth_NoAuth_Returns200` | auth-service |
| TC108 | `actuatorInfo_NoAuth_Returns200` | auth-service |
| TC109 | `actuatorHealth_NoAuth_Returns200` | user-service |
| TC110 | `actuatorInfo_NoAuth_Returns200` | user-service |

**Files đã sửa/tạo:**
- `core/src/main/java/com/r2s/core/config/SecurityConstants.java`
- `user-service/src/main/java/com/r2s/user/config/SecurityConfig.java`
- `auth-service/src/main/resources/application.properties`
- `user-service/src/main/resources/application.properties`
- `auth-service/src/test/java/com/r2s/auth/security/ActuatorSecuritySmokeTest.java` (mới)
- `user-service/src/test/java/com/r2s/user/security/ActuatorSecuritySmokeTest.java` (mới)

---

## P3 — Maven duplicate dependencies

### Trước khi sửa (vi phạm)

`user-service/pom.xml`:
```xml
<!-- Lan 1 -->
<dependency><groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
<dependency><groupId>com.h2database</groupId>
    <artifactId>h2</artifactId><scope>test</scope></dependency>

<!-- ... -->

<!-- Lan 2 (TRUNG!) -->
<dependency><groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
<dependency><groupId>com.h2database</groupId>
    <artifactId>h2</artifactId><scope>test</scope></dependency>
```

### Sau khi sửa

Đã xoá block dependency trùng ở cuối file `user-service/pom.xml`. Còn lại 1 lần duy nhất mỗi dependency.

**File đã sửa:**
- `user-service/pom.xml`

---

## Verification cuối

### Build sạch

```bash
mvn clean install -DskipTests
# BUILD SUCCESS (không còn warning duplicate dependency)
```

### Tests pass

```bash
mvn test
```

Kết quả:
- `auth-service`: **44 tests pass** (36 cũ + 6 RateLimitFilter + 2 Actuator smoke)
- `user-service`: **28 tests pass** (26 cũ + 2 Actuator smoke)
- **Tổng: 72 automated tests pass**
- Ngoại lệ: 2 E2E `AuthFlowE2ETest` vẫn fail trên Windows do `@EmbeddedKafka` không mở được loopback connection (vấn đề môi trường, không phải code) — đã ghi nhận trong `Testing_Guide.md`.

### Cấu hình production fail-fast

Test thử (mô phỏng): chạy app không set bất kỳ env nào:
```bash
java -jar auth-service.jar    # không có SPRING_PROFILES_ACTIVE
```
→ Spring Boot fail-fast vì:
- `application.properties` không có default profile → vào "default" profile
- Profile "default" không có `spring.datasource.url` → fail to start
- Profile "prod" yêu cầu `${JWT_SECRET}` không fallback → fail-fast nếu thiếu env

---

## Còn lại — chưa làm trong Round 2 (ghi nhận để Round 3)

1. **Rate limit shared storage:** Vẫn là in-memory. Nếu scale > 1 instance, cần dùng Redis hoặc API gateway. Đã document trong code.
2. **Actuator separate port:** Mới làm permit-policy thống nhất. Bước nâng cao là tách `management.server.port=9081/9082` riêng và không expose ra host — chỉ scrape từ trong docker network. Đã document trong `SecurityConstants.java`.
3. **`@MockBean` deprecated:** Spring Boot 3.4+ khuyến nghị `@MockitoBean`. Hiện đang dùng `@MockBean` (vẫn chạy được, có warning). Migrate vào Round 3.
4. **2 E2E EmbeddedKafka fail trên Windows:** Cần annotate `@DisabledOnOs(OS.WINDOWS)` hoặc convert sang Testcontainers Kafka.

---

## Tóm tắt nhanh cho thầy

| Finding của thầy | Code đã sai chỗ nào | Đã sửa thế nào |
|---|---|---|
| P1.1 | `spring.profiles.active=dev` trong `application.properties` + fallback secret trong dev profile + `ddl-auto=update` | Xoá default profile, đổi dev sang `ddl-auto=none`, thêm `application-prod.properties` thuần fail-fast |
| P1.2 | V2 dùng `DROP TABLE users` xoá sạch dữ liệu | Viết lại V2 dạng `ALTER TABLE` + backfill role từ M2M + đổi kiểu id qua `id_new` BIGSERIAL — preserve toàn bộ user data |
| P2.1 | Map bucket không TTL, dùng `request.getRemoteAddr()` không xử lý proxy | Thêm `BucketEntry` với `lastAccess`, scheduled cleanup mỗi phút, đọc X-Forwarded-For chỉ khi đến từ trusted proxy, 6 tests mới |
| P2.2 | `user-service/SecurityConfig` không permit `/actuator/**` | Thêm `ACTUATOR_PUBLIC_URLS` chung trong `core/SecurityConstants`, cả 2 service cùng dùng, 4 smoke tests mới |
| P3 | `user-service/pom.xml` khai báo trùng `spring-security-test` và `h2` | Xoá block dependency trùng ở cuối pom |

**Production gate:** Đã đáp ứng các blocker P1.x. Code có thể tiến tới production sau khi:
- Test migration V2 trên DB có dữ liệu sample (test team)
- Set up env `SPRING_PROFILES_ACTIVE=prod` + đầy đủ env biến trên server
- (Nâng cao) Tách actuator sang separate management port

---

**Author:** Nguyễn Hiếu An
**Date:** May 20, 2026
