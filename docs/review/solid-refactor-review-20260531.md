# Review chi tiết SOLID Refactor — 2026-05-31

## Phạm vi
- Project: User Access Management System
- Đối chiếu với:
  - `docs/review/solid-refactor-proposal.md`
  - `docs/review/solid-refactor-short-report.md`
- Mục tiêu: kiểm tra các thay đổi refactor SOLID/Design Patterns/Clean Code đã đúng với proposal và báo cáo hoàn thành chưa.

## Kết luận
Refactor đã đi đúng hướng và phần lớn cấu trúc chính đã được triển khai:
- Auth service đã tách theo các package `authentication`, `registration`, `password`, `role`.
- User service đã tách theo các package `profile`, `management`, `validation`.
- Controller inject interface thay vì concrete service.
- Strategy Pattern cho login đã có.
- `ApiResponse` và `ResponseBuilder` đã được dùng ở controller.
- Test suite pass khi chạy ngoài sandbox.

Tuy nhiên, vẫn còn một số điểm chưa khớp hoàn toàn với mục tiêu trong proposal/report, đặc biệt là response format chưa consistent ở security filters và coverage gate chưa bao phủ các phần refactor quan trọng.

## Findings

### P1 — Response format vẫn chưa consistent như proposal/report cam kết

**Vị trí:**
- `auth-service/src/main/java/com/r2s/auth/security/JwtFilter.java`
- `user-service/src/main/java/com/r2s/user/security/JwtFilter.java`
- `auth-service/src/main/java/com/r2s/auth/security/RateLimitFilter.java`

**Hiện trạng:**
`JwtFilter` của cả 2 service vẫn tự ghi plain text khi token lỗi:

```java
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
response.getWriter().write("Invalid or expired token");
return;
```

`RateLimitFilter` cũng trả JSON thủ công chỉ có `message`:

```java
response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
response.setContentType("application/json");
response.getWriter().write(
        "{\"message\": \"Too many login attempts. Please try again later.\"}");
```

**Rủi ro:**
- Response không có format thống nhất `{success, data, message, timestamp}` như `ApiResponse`.
- Frontend vẫn phải parse nhiều kiểu response khác nhau.
- Không đúng cam kết trong `solid-refactor-short-report.md` rằng error response đã consistent.

**Yêu cầu điều chỉnh:**
- JWT invalid/expired nên trả `ApiResponse.error(...)` với `Content-Type: application/json`.
- Rate limit 429 cũng nên trả `ApiResponse.error(...)`.
- Cân nhắc dùng chung một helper/writer cho security filter response để tránh lặp logic giữa auth-service và user-service.
- Bổ sung test assert body JSON có `success=false`, `message`, `timestamp` cho JWT invalid và rate limit 429.

### P2 — Coverage gate đạt nhưng chưa chứng minh đủ phần refactor API/security

**Vị trí:**
- `pom.xml`

**Hiện trạng:**
JaCoCo đang exclude nhiều package quan trọng:

```xml
<exclude>**/dto/**</exclude>
<exclude>**/entity/**</exclude>
<exclude>**/config/**</exclude>
<exclude>**/*Application*</exclude>
<exclude>**/security/**</exclude>
<exclude>**/controller/**</exclude>
```

Trong khi refactor lần này chạm mạnh vào:
- Controller response format.
- Security handlers/filter.
- DTO `LoginRequest.authType`.
- Actuator/security policy.

**Rủi ro:**
- Claim “coverage gate >= 85% đạt” là đúng theo cấu hình hiện tại, nhưng gate quá hẹp.
- Các phần rủi ro nhất của refactor không bị coverage gate kiểm soát.
- Reviewer có thể hiểu nhầm coverage đã bảo chứng toàn bộ refactor.

**Yêu cầu điều chỉnh:**
- Không nhất thiết phải include toàn bộ controller/security vào JaCoCo gate ngay, nhưng cần tách rõ trong report: coverage gate hiện chỉ đo phần service/business logic.
- Bổ sung integration/smoke tests assert response body cho controller/security filter.
- Cân nhắc bỏ exclude `controller` hoặc `security` nếu muốn claim coverage phản ánh refactor API/security.

### P2 — Exception handling bị phân tán và trùng trách nhiệm

**Vị trí:**
- `core/src/main/java/com/r2s/core/exception/GlobalExceptionHandler.java`
- `auth-service/src/main/java/com/r2s/auth/exception/AuthExceptionHandler.java`

**Hiện trạng:**
`GlobalExceptionHandler` trong core đã xử lý:
- `DuplicateUsernameException`
- `DuplicateEmailException`
- `BusinessException`
- `BadCredentialsException`
- `UsernameNotFoundException`
- `AccessDeniedException`

Nhưng `auth-service` vẫn có `AuthExceptionHandler` xử lý lại một số nhóm lỗi tương tự:
- `DuplicateUsernameException`
- `IllegalArgumentException`
- `BadCredentialsException`
- `UsernameNotFoundException`
- `AccessDeniedException`

**Rủi ro:**
- Giảm lợi ích của centralized exception handling.
- Dễ lệch status/message/body giữa các service khi sửa về sau.
- Khó biết handler nào đang là source of truth.

**Yêu cầu điều chỉnh:**
- Chọn một hướng rõ ràng:
  - Dùng `GlobalExceptionHandler` trong core làm handler chung cho cả hệ thống; hoặc
  - Mỗi service có handler riêng nhưng phải giới hạn phạm vi rõ ràng và tránh duplicate handler cho cùng exception.
- Nếu giữ `AuthExceptionHandler`, cần giải thích vì sao auth-service cần override handler chung.
- Bổ sung test để đảm bảo duplicate username, bad credentials, access denied trả cùng format/status như mong muốn.

### P3 — Báo cáo test không khớp trạng thái hiện tại

**Vị trí:**
- `docs/review/solid-refactor-short-report.md`
- `auth-service/src/test/java/com/r2s/auth/e2e/AuthFlowE2ETest.java`

**Hiện trạng:**
Report ghi:
- `72/72` tests pass.
- Loại trừ 2 E2E tests `AuthFlowE2ETest`.

Kết quả kiểm chứng hiện tại:
- `mvn test` chạy ngoài sandbox: build success.
- `auth-service`: 47 tests pass.
- `user-service`: 27 tests pass.
- Tổng: 74 tests pass.
- `AuthFlowE2ETest` vẫn chạy và pass.

**Rủi ro:**
- Báo cáo không còn phản ánh đúng trạng thái code hiện tại.
- Reviewer/Fresher có thể hiểu nhầm E2E đang bị exclude trong khi thực tế vẫn chạy.

**Yêu cầu điều chỉnh:**
- Cập nhật `solid-refactor-short-report.md` hoặc tạo phụ lục cập nhật test results.
- Ghi rõ E2E hiện tại có chạy hay không chạy trong pipeline/local.
- Nếu muốn exclude E2E, cần cấu hình Maven profile/tag rõ ràng thay vì chỉ ghi trong report.

### P3 — Build chưa thật sự “sạch” nếu xét warning

**Hiện trạng khi chạy test/verify:**
- `mvn test` pass khi chạy ngoài sandbox.
- Vẫn có warning Mockito self-attach/dynamic agent.
- Một số test dùng `@MockBean` đã deprecated trong Spring Boot mới.
- Có warning Hibernate dialect khi test.
- `RateLimitFilter` dùng hoặc override API deprecated của Bucket4j.

**Rủi ro:**
- Không block refactor hiện tại, nhưng chưa nên gọi là build sạch tuyệt đối.
- Về lâu dài có thể fail khi nâng JDK/Spring Boot/Maven plugin.

**Yêu cầu điều chỉnh:**
- Thay `@MockBean` deprecated theo hướng khuyến nghị của Spring Boot version đang dùng.
- Rà lại cấu hình Hibernate dialect trong test profile.
- Cập nhật cách dùng Bucket4j API để hết deprecated warning.
- Với Mockito trên JDK mới, cân nhắc cấu hình Mockito Java agent thay vì self-attach runtime.

## Điểm đã làm đúng
- Tách `AuthenticationService` cũ thành các interface/service nhỏ hơn theo ISP/SRP.
- `RegistrationServiceImpl` phụ thuộc `PasswordService` abstraction thay vì xử lý password trực tiếp.
- `AuthServiceImpl` dùng danh sách `AuthenticationStrategy`, mở đường thêm OAuth/SAML/2FA mà không sửa flow chính.
- `UserProfileService`, `UserManagementService`, `UserValidationService` đã tách trách nhiệm rõ hơn.
- Controller đã chuyển sang trả `ApiResponse` qua `ResponseBuilder`.
- Duplicate username/email đã có domain exception riêng và map sang 409 Conflict.
- Test có thêm nhóm mới cho service split, rate limit, actuator security smoke.

## Ghi chú kiểm chứng

Đã chạy trong sandbox:

```bash
mvn test
```

Kết quả:
- Fail do Mockito/ByteBuddy không self-attach được trong sandbox.
- Đây là vấn đề quyền môi trường test, không kết luận source code fail.

Đã chạy lại ngoài sandbox:

```bash
mvn test
```

Kết quả:
- Build success.
- `auth-service`: 47 tests pass.
- `user-service`: 27 tests pass.
- Tổng: 74 tests pass.

Đã chạy:

```bash
mvn verify -DskipTests
```

Kết quả:
- Build success.
- JaCoCo check báo `All coverage checks have been met`.
- Lưu ý: lệnh này dùng execution data đã có từ lần chạy test trước và coverage exclude nhiều package quan trọng như đã nêu ở finding P2.

## Kết luận review
Không có dấu hiệu refactor làm fail build/test chính khi chạy đúng môi trường. Tuy nhiên, chưa chốt “hoàn tất hoàn toàn theo proposal” vì response format vẫn chưa consistent ở security filters/rate limit, report test chưa khớp thực tế, và coverage claim cần diễn giải cẩn thận hơn.

Ưu tiên sửa:
1. Chuẩn hóa response body cho JWT invalid/expired và rate limit 429 theo `ApiResponse`.
2. Cập nhật test/report để khớp số lượng test thực tế.
3. Làm rõ chiến lược exception handler chung vs riêng.
4. Bổ sung test body response cho các path security/error.
5. Dọn các warning build/test còn lại.
