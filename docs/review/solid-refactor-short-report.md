# Short Report — SOLID Refactor Hoàn Thành

**Project:** User Access Management System
**Người thực hiện:** Nguyễn Hiếu An
**Ngày báo cáo:** 28/05/2026 (cập nhật 01/06/2026 theo review của thầy)
**Status:** ✅ COMPLETED — đã áp dụng follow-up review 2026-05-31

> **Cập nhật 01/06/2026:** Đã xử lý 5 findings trong
> `docs/review/solid-refactor-review-20260531.md`. Xem **Section 11 — Follow-up review fixes**
> ở cuối tài liệu. Các số liệu test ở Section 3 đã được cập nhật cho khớp trạng thái hiện tại.

---

## 1. Tóm tắt

Đã hoàn thành toàn bộ refactor theo proposal đã được thầy duyệt + áp dụng feedback của thầy về **package organization theo chức năng**.

Phạm vi thực hiện:
- ✅ 5 nhóm refactor theo proposal (ISP/DIP/Strategy/ApiResponse/Tests)
- ✅ Thêm: tổ chức package theo chức năng (theo yêu cầu bổ sung của thầy)
- ✅ Verify build sạch + tất cả tests pass + coverage gate đạt

---

## 2. Cấu trúc package theo chức năng (theo feedback thầy)

### auth-service
```
com.r2s.auth.service/
├── authentication/
│   ├── AuthenticationService.java       (interface)
│   └── AuthServiceImpl.java             (impl, dùng Strategy Pattern)
├── registration/
│   ├── RegistrationService.java
│   └── RegistrationServiceImpl.java     (phụ thuộc PasswordService - DIP)
├── password/
│   ├── PasswordService.java
│   └── PasswordServiceImpl.java         (wrap BCryptPasswordEncoder)
└── role/
    ├── RoleManagementService.java
    └── RoleManagementServiceImpl.java   (admin-only logic)

com.r2s.auth.strategy/
├── AuthenticationStrategy.java          (interface - Strategy Pattern)
└── PasswordAuthenticationStrategy.java  (default impl)
```

### user-service
```
com.r2s.user.service/
├── profile/
│   ├── UserProfileService.java
│   └── UserProfileServiceImpl.java     (read/update profile - SRP)
├── management/
│   ├── UserManagementService.java
│   └── UserManagementServiceImpl.java  (admin delete - SRP)
└── validation/
    ├── UserValidationService.java
    └── UserValidationServiceImpl.java   (validation tập trung - DRY)
```

→ Mỗi package chỉ 2 file (interface + impl) → dễ navigate, không bị "rối mắt".

---

## 3. Test Coverage

### Test pass (cập nhật 01/06/2026)

```
Module          | Tests | Pass | Fail | Ghi chú
----------------|-------|------|------|------------------------------
core            |  26   |  26  |  0   | Exception/Response/Writer/Logger tests
auth-service    |  43   |  43  |  0   | gồm 2 E2E (AuthFlowE2ETest)
user-service    |  27   |  27  |  0   |
──────────────────────────────────────────────────────────────────
Total (đầy đủ)  |  96   |  96  |  0   ✅ (chạy trên Linux/CI có Kafka)
```

**Về E2E `AuthFlowE2ETest` (2 tests):**
- ✅ **CHẠY ĐƯỢC + PASS** trên Linux/GitLab CI runner (EmbeddedKafka hoạt động bình thường).
- ⚠️ Trên **Windows local**: EmbeddedKafka không mở được loopback connection → khi cần
  chạy local trên Windows, dùng lệnh loại trừ tạm thời:
  ```bash
  mvn test -Dtest='!AuthFlowE2ETest' -Dsurefire.failIfNoSpecifiedTests=false
  ```
  → khi đó: core 26 + auth 41 + user 27 = **94 tests pass**.
- Đây là giới hạn môi trường Windows, **không phải** E2E bị exclude khỏi pipeline.
  Pipeline GitLab vẫn chạy đầy đủ 96 tests.

> **So với báo cáo trước (72 tests):** số tăng lên 96 do (1) thêm 26 test cho
> core (GlobalExceptionHandler + ApiResponse + ResponseBuilder + ApiResponseWriter
> + LoggerUtil), (2) thêm test assert body JSON cho JWT/rate-limit, (3) E2E giờ
> được tính vào tổng (chạy trên CI). Đồng thời đã **xóa** `AuthExceptionHandlerTest`
> (5 tests) khi gộp về handler chung.
>
> **Lưu ý JaCoCo:** sau khi thêm test cho core, JaCoCo bắt đầu đo coverage module
> core (trước đây skip vì không có test). Đã thêm exclude `**/event/**` (data-carrier
> như dto/entity) và viết đủ test để core đạt LINE ≥ 85%.

### JaCoCo Coverage Gate

```
Required: LINE coverage ≥ 85%
Actual:   All coverage checks have been met ✅
```

→ **Coverage KHÔNG giảm** sau refactor. Tests đầy đủ cover các flow mới (Strategy Pattern, 3 service impl mới, exception handler mới).

### Test breakdown (auth-service) — cập nhật 01/06/2026

| Test class | Count | Pattern |
|---|---|---|
| AuthServiceTest (@Nested: Registration/Authentication/RoleMgmt/Password) | 19 | TestDataBuilder + @Nested |
| AuthControllerTest (@Nested: Register/Login endpoint) | 4 | MockMvc + ApiResponse assertion |
| AuthServiceIntegrationTest | 2 | Inject `RegistrationService` interface (DIP) |
| JwtFilterTest | 4 | Assert body ApiResponse cho JWT invalid (P1) |
| RateLimitFilterTest | 7 | Assert body ApiResponse cho 429 (P1) |
| AuthFlowE2ETest | 2 | E2E (chạy trên CI/Linux) |
| Other (Actuator, Kafka, Moderator) | 5 | Existing |
| **Tổng** | **43** | |

> `AuthExceptionHandlerTest` (5 tests) đã **xóa** — handler này gộp về
> `GlobalExceptionHandler` ở core (xem Section 11, P2). Test tương đương
> chuyển sang `core/GlobalExceptionHandlerTest` (10 tests).

### Test breakdown (user-service)

| Test class | Count | Pattern |
|---|---|---|
| UserServiceTest (@Nested: Profile/Management/DtoSecurity) | 17 | Split impl + TestDataBuilder |
| UserControllerTest | 2 | MockMvc + ApiResponse assertion |
| UserServiceIntegrationTest | 2 | Inject 2 interfaces riêng (ISP) |
| Other (Kafka, Actuator) | 6 | Existing |
| **Tổng** | **27** | |

---

## 4. API Response Format Verification

### Trước refactor (inconsistent)

```bash
POST /auth/register
→ Body: "User registered successfully"   (text thuần)

POST /auth/login
→ Body: {"token":"..."}                   (AuthResponse object)

GET /users (admin)
→ Body: [{...}, {...}]                    (List)
```

### Sau refactor (consistent ApiResponse wrapper)

**Success case:**
```bash
$ curl -X POST http://localhost:8081/auth/register -d '...'
```
```json
{
  "success": true,
  "data": { "token": "eyJ..." },
  "message": "User registered successfully",
  "timestamp": "2026-05-28T22:17:00"
}
```

**Error case (BadCredentials):**
```bash
$ curl -X POST http://localhost:8081/auth/login -d '{wrong_password}'
```
```json
{
  "success": false,
  "message": "Invalid username or password",
  "timestamp": "2026-05-28T22:17:30"
}
```
Status: 401 Unauthorized ✅

**Error case (Duplicate username):**
```json
{
  "success": false,
  "message": "Username already exists: alice",
  "timestamp": "2026-05-28T22:17:45"
}
```
Status: 409 Conflict ✅ (đúng semantic theo RFC 7231)

---

## 5. E2E Verification (Postman)

| Collection | Tests | Pass | Fail |
|---|---|---|---|
| UAM - Auth Service E2E | 8 | 8 | 0 ✅ |
| UAM - User Service E2E | 10 | 10 | 0 ✅ |
| **Tổng** | **18** | **18** | **0** |

Mọi test case (happy path + error path) đều verify được:
- Status code đúng (200/201/400/401/403/409)
- ApiResponse wrapper format (`success`/`data`/`message`/`timestamp`)
- Token JWT extract + reuse được giữa các test

---

## 6. SOLID Principles Compliance

| Nguyên tắc | Trước refactor | Sau refactor |
|---|---|---|
| **S** - SRP | `AuthService` có 3 trách nhiệm | Tách thành 4 class chuyên trách |
| **O** - OCP | `login()` hard-code 1 cách | Strategy Pattern (thêm OAuth = thêm class) |
| **L** - LSP | OK | OK |
| **I** - ISP | 1 interface gom 3-4 method | 4 interface, mỗi cái 1-3 method |
| **D** - DIP | Service phụ thuộc `PasswordEncoder` (concrete) | Phụ thuộc `PasswordService` (abstraction) |

---

## 7. Design Patterns đã áp dụng

| Pattern | Vị trí | Tác dụng |
|---|---|---|
| **Repository** | Spring Data JPA | Tách persistence layer |
| **Strategy** | `AuthenticationStrategy` + impl | Thêm OAuth/SAML không sửa code cũ |
| **Observer** | Kafka events (UserRegistered/Deleted) | Decouple producer + consumer |
| **Builder** | `ApiResponse.builder()` + `TestDataBuilder` | Tạo object phức tạp dễ đọc |
| **Factory Method** | `ApiResponse.success()`, `ApiResponse.error()` | Static factory cho object construction |

---

## 8. Files đụng đến

| Loại | Số lượng |
|---|---|
| File mới tạo | 14 |
| File modified | 10 |
| File xóa (replaced) | 2 |
| **Tổng files đụng** | **26** |

Chi tiết phân loại:
- **core module:** ApiResponse, ResponseBuilder, BusinessException, DuplicateUsernameException, DuplicateEmailException, GlobalExceptionHandler (update)
- **auth-service:** 4 service packages mới (8 files) + AuthController + Strategy + Tests
- **user-service:** 3 service packages mới (6 files) + UserController + Tests + SecurityConfig

> **Lưu ý (sau follow-up 31/05):** ngoài các file refactor ban đầu, đợt fix follow-up
> còn **thêm** `ApiResponseWriter` + `GlobalExceptionHandlerTest` (và 4 test core),
> **xóa** `AuthExceptionHandler` + `AuthExceptionHandlerTest` (gộp về handler chung).
> Chi tiết ở Section 11.

---

## 9. Trade-off thực tế nhận được

| Cải thiện | Trade-off chấp nhận |
|---|---|
| ✅ Code dễ test (mock interface dễ hơn concrete) | Số file tăng 14 cái |
| ✅ Mở rộng dễ (thêm OAuth ko sửa code cũ) | Cấu trúc package phức tạp hơn |
| ✅ Response format consistent → frontend code 1 lần | Phải refactor controllers + tests |
| ✅ HTTP status code đúng semantic (409 vs 400) | Phải update GlobalExceptionHandler |
| ✅ Test coverage không giảm (≥ 85%) | Tests phải rewrite theo @Nested |

---

## 10. Kết luận

✅ **Refactor hoàn tất theo đúng proposal + feedback thầy + follow-up review 31/05.**
- Build sạch (BUILD SUCCESS)
- **96/96** unit + integration + E2E tests pass (cập nhật sau follow-up — xem Section 11)
- 18/18 Postman E2E tests pass
- Coverage gate ≥ 85% đạt cả 3 module
- Cấu trúc package theo chức năng (1 package = 1 chức năng = 2 files)
- Exception handling tập trung 1 source of truth (xem Section 11 — P2)

**Sẵn sàng cho thầy review chi tiết.**

---

## 11. Follow-up review fixes (01/06/2026)

Xử lý 5 findings trong `docs/review/solid-refactor-review-20260531.md`:

### P1 — Response format consistent ở security filters ✅

**Trước:** `JwtFilter` (cả 2 service) trả plain text `"Invalid or expired token"`;
`RateLimitFilter` trả JSON thủ công chỉ có `message`.

**Sau:**
- Tạo `core/response/ApiResponseWriter.java` — helper **dùng chung** cho mọi security
  filter ghi `ApiResponse` format (DRY giữa auth-service và user-service).
- `JwtFilter` (auth + user): JWT invalid/expired → `ApiResponse.error(...)` JSON.
- `RateLimitFilter`: 429 → `ApiResponse.error(...)` JSON đầy đủ `{success,message,timestamp}`.
- 4 security handler (EntryPoint + AccessDeniedHandler × 2 service) cũng dùng chung
  `ApiResponseWriter` → loại bỏ code lặp.
- **Test mới:** assert body JSON có `success=false`/`message`/`timestamp` cho
  JWT invalid (JwtFilterTest TC049/TC050) và rate limit 429 (RateLimitFilterTest TC107).

### P2 — Exception handling bị phân tán và trùng trách nhiệm ✅

**Hiện trạng (trước fix):** có 2 `@RestControllerAdvice` xử lý chồng chéo nhau:

| Exception | GlobalExceptionHandler (core) | AuthExceptionHandler (auth-service) |
|---|---|---|
| DuplicateUsernameException | ✅ → 409 | ✅ → 409 (trùng) |
| DuplicateEmailException | ✅ → 409 | — |
| BusinessException | ✅ → 400 | — |
| IllegalArgumentException | ✅ → 400 | ✅ → 400 (trùng) |
| BadCredentialsException | ✅ → 401 | ✅ → 401 (trùng) |
| UsernameNotFoundException | ✅ → 401 | ✅ → 401 (trùng) |
| AccessDeniedException | ✅ → 403 | ✅ → 403 (trùng) |

→ Rủi ro đúng như thầy nêu: giảm lợi ích centralized handling, dễ lệch
status/message/body khi sửa về sau, khó biết handler nào là source of truth.

**Quyết định:** chọn **Hướng 1 — dùng `GlobalExceptionHandler` ở core làm handler
chung cho cả hệ thống** (không chọn hướng giữ handler riêng từng service).

**Lý do chọn hướng 1:**
- `GlobalExceptionHandler` đã cover **đầy đủ** mọi exception mà `AuthExceptionHandler`
  xử lý (xem bảng trên) → `AuthExceptionHandler` hoàn toàn dư thừa.
- Cả 2 service đều khai báo `@SpringBootApplication(scanBasePackages = {... "com.r2s.core"})`
  nên `GlobalExceptionHandler` ở core **tự động active** cho cả auth-service lẫn
  user-service → 1 nguồn duy nhất, không lệch format giữa các service.
- auth-service **không có nhu cầu đặc thù** nào cần override handler chung → không
  có lý do giữ `AuthExceptionHandler`.

**Đã làm:**
- **Xóa** `auth-service/.../exception/AuthExceptionHandler.java` (+ `AuthExceptionHandlerTest.java`).
- Verify: toàn hệ thống giờ chỉ còn **1** `@RestControllerAdvice` duy nhất tại
  `core/GlobalExceptionHandler.java`.
- (Custom `AuthenticationEntryPoint`/`AccessDeniedHandler` ở security KHÔNG phải
  `@RestControllerAdvice` — chúng xử lý exception ở tầng filter, không trùng vai trò.)

**Test bổ sung (theo yêu cầu thầy) — `core/GlobalExceptionHandlerTest` (10 tests):**

| Test | Exception | Kỳ vọng |
|---|---|---|
| GEH01 | DuplicateUsernameException | 409 Conflict + success=false |
| GEH02 | DuplicateEmailException | 409 Conflict |
| GEH03 | BusinessException | 400 Bad Request |
| GEH04 | IllegalArgumentException | 400 Bad Request |
| GEH05 | **BadCredentialsException** | **401 + "Invalid username or password" (no info leak)** |
| GEH06 | UsernameNotFoundException | 401 + generic message |
| GEH07 | **AccessDeniedException** | **403 Forbidden + "Access denied"** |
| GEH08 | Generic Exception | 500 + "An unexpected error occurred" (no stacktrace leak) |
| GEH09 | CustomException (legacy) | 400 Bad Request |
| GEH10 | Domain exception messages | message + cause đúng |

→ 3 case thầy quan tâm (**duplicate username** GEH01, **bad credentials** GEH05,
**access denied** GEH07) đều được assert cùng format `ApiResponse` + đúng HTTP status.
Vì cả 2 service dùng chung handler này nên format/status **đồng nhất** giữa các service.

### P2 — Coverage gate: diễn giải rõ phạm vi ✅

- JaCoCo gate hiện **chỉ đo service/business logic** (exclude `dto/entity/config/
  security/controller`). Đây là **chủ ý** — không claim coverage bao phủ toàn bộ refactor.
- Phần controller/security được bảo chứng bằng **integration + smoke tests**
  (AuthControllerTest, UserControllerTest, ActuatorSecuritySmokeTest, JwtFilterTest,
  RateLimitFilterTest) assert trực tiếp response body/status.
- Gate giữ nguyên LINE ≥ 85% cho phần business logic.

### P3 — Report khớp số test thực tế ✅

- Đã cập nhật Section 3: **96 tests** (core 26 + auth 43 + user 27), E2E ghi rõ
  chạy trên CI/Linux, lệnh loại trừ trên Windows local.

### P3 — Dọn build warnings ✅

- `@MockBean` deprecated → `@MockitoBean` (Spring Boot 3.4) ở 4 test file.
- `RateLimitFilter`: Bucket4j `Bandwidth.classic()` + `Refill.greedy()` deprecated →
  `Bandwidth.builder().capacity(...).refillGreedy(...)` (API mới).
- Hibernate dialect: bỏ `spring.jpa.properties.hibernate.dialect` /
  `database-platform` explicit ở mọi properties → Hibernate 6 tự detect từ JDBC
  connection (hết warning HHH90000025).

### Vấn đề phát sinh trong lúc verify trên CI (đã xử lý)

Trong quá trình verify trên GitLab CI runner (Linux), phát sinh 2 vấn đề — đã fix:

1. **E2E `AuthFlowE2ETest` assert sai status (201 vs 200):**
   Sau refactor, `POST /auth/register` trả **201 CREATED** (đúng REST semantic, qua
   `responseBuilder.buildCreatedResponse(...)`), nhưng test cũ còn assert `200 OK`.
   → Local Windows skip E2E (EmbeddedKafka lỗi loopback) nên không phát hiện;
   GitLab Linux chạy được EmbeddedKafka mới lộ. Đã sửa assert `200 OK` → `201 CREATED`.
   (commit `fix(test): update AuthFlowE2ETest expect 201 CREATED`).

2. **JaCoCo coverage gate fail ở module `core`:**
   Sau khi thêm test vào core, JaCoCo bắt đầu đo core (trước đây skip vì không có
   test) → 36% < 85% → pipeline fail. Đã fix bằng cách viết đủ test cho core
   (ApiResponse/ResponseBuilder/ApiResponseWriter/LoggerUtil + mở rộng
   GlobalExceptionHandlerTest) + exclude `**/event/**` → core đạt ≥ 85%.

> **Bài học:** verify bằng `mvn clean install` (có JaCoCo `check`) thay vì chỉ
> `mvn test` — để bắt coverage gate fail giống môi trường CI trước khi push.

**Kết quả cuối (sau follow-up + xử lý phát sinh):** Build SUCCESS, **96/96 tests pass**
(core 26 + auth 43 + user 27), coverage gate ≥ 85% đạt cả 3 module, pipeline
GitLab build + test xanh, ít warning hơn.

---

**Author:** Nguyễn Hiếu An
**Date:** May 28, 2026 (updated June 1, 2026)
**Status:** ✅ Follow-up review fixes applied
