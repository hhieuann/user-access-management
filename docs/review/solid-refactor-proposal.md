# Đề xuất Refactor SOLID / Design Patterns / Clean Code

**Project:** User Access Management System
**Người thực hiện:** Nguyễn Hiếu An
**Ngày đề xuất:** 26/05/2026
**Reference:** PDF Phần 10 - "Áp dụng các nguyên tắc SOLID, Design Patterns và Clean code"

---

## 📌 Mục đích tài liệu này

Em đã review code hiện tại của project, so sánh với yêu cầu Phần 10 PDF và identified các điểm cần refactor. Tài liệu này **đề xuất** các thay đổi cụ thể, **chưa thực hiện** — em xin thầy xem qua và xác nhận trước khi em tiến hành refactor + commit.

**Phạm vi đề xuất:**
- 5 nhóm thay đổi chính (ISP, DIP, Strategy Pattern, ApiResponse, Tests)
- ~24 file sẽ đụng đến
- Backwards compatible với existing tests (sau khi update test)

---

## 🗺 Tổng quan đề xuất

| # | Vi phạm hiện tại | Đề xuất sửa | Mức độ |
|---|---|---|---|
| 1 | `AuthenticationService` gom 3 chức năng (register/login/assignRole) | Tách thành 4 interface chuyên trách | 🔴 Cao |
| 2 | `UserManagementService` gom 4 method (query + command + profile + management) | Tách thành 3 interface | 🔴 Cao |
| 3 | Service phụ thuộc concrete `PasswordEncoder` (BCrypt) | Tạo `PasswordService` abstraction | 🟡 Trung |
| 4 | Không có Strategy Pattern cho authentication | Thêm `AuthenticationStrategy` + impl | 🟡 Trung |
| 5 | Response format không consistent (string thuần / object) | Tạo `ApiResponse` wrapper + `ResponseBuilder` | 🟡 Trung |
| 6 | Generic `IllegalArgumentException` cho duplicate | Tạo `DuplicateUsernameException` + `DuplicateEmailException` | 🟢 Nhỏ |
| 7 | Tests không group theo chức năng, magic strings lặp | `@Nested` + `TestDataBuilder` | 🟢 Nhỏ |

---

## 🎯 Đề xuất 1: Tách `AuthenticationService` (ISP)

### Vi phạm hiện tại

`auth-service/src/main/java/com/r2s/auth/service/AuthenticationService.java`:
```java
public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);    // Registration concern
    AuthResponse login(LoginRequest request);          // Authentication concern
    void assignRole(String username, Role role);       // Authorization/Admin concern
}
```

**Vi phạm SRP + ISP:**
- 1 interface có 3 lý do để thay đổi (đổi register, đổi login, đổi admin)
- Mobile app chỉ cần login bị buộc phụ thuộc cả `register` + `assignRole`
- Khó test: mock 1 method phải mock toàn bộ

### Đề xuất sửa

Tách thành **4 interface chuyên trách**:

```java
// AuthenticationService.java - CHỈ login
public interface AuthenticationService {
    AuthResponse login(LoginRequest request);
}

// RegistrationService.java - CHỈ register
public interface RegistrationService {
    AuthResponse register(RegisterRequest request);
}

// PasswordService.java - CHỈ password operations
public interface PasswordService {
    String encodePassword(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}

// RoleManagementService.java - CHỈ admin role
public interface RoleManagementService {
    void assignRole(String username, Role role);
}
```

Mỗi interface có 4 implementation class riêng trong package `service/impl/`.

**Lợi ích:**
- Mỗi class có 1 lý do duy nhất để thay đổi
- Controller chỉ inject interface cần thiết (DIP)
- Test isolated: mock chính xác cái cần

**File ảnh hưởng:**
- `auth-service/service/AuthenticationService.java` (rewrite)
- `auth-service/service/RegistrationService.java` (new)
- `auth-service/service/PasswordService.java` (new)
- `auth-service/service/RoleManagementService.java` (new)
- `auth-service/service/impl/AuthServiceImpl.java` (new)
- `auth-service/service/impl/RegistrationServiceImpl.java` (new)
- `auth-service/service/impl/PasswordServiceImpl.java` (new)
- `auth-service/service/impl/RoleManagementServiceImpl.java` (new)
- `auth-service/service/AuthService.java` (delete - đã tách)
- `auth-service/controller/AuthController.java` (inject 3 interface riêng)

---

## 🎯 Đề xuất 2: Tách `UserManagementService` (ISP)

### Vi phạm hiện tại

`user-service/src/main/java/com/r2s/user/service/UserManagementService.java`:
```java
public interface UserManagementService {
    // Query
    List<UserResponse> getAllUsers();
    UserResponse getUserByUsername(String username);
    // Command
    UserResponse updateUser(String username, UpdateUserRequest request);
    void deleteUser(String username);
}
```

**Vi phạm ISP:** trộn 2 trách nhiệm (profile ops + management ops) → User profile controller bị buộc phụ thuộc `deleteUser` (admin only) dù không dùng.

### Đề xuất sửa

Theo đúng PDF Phần 10 trang 44, tách thành **3 interface**:

```java
// UserProfileService.java - profile của user
public interface UserProfileService {
    List<UserResponse> getAllUsers();
    UserResponse getUserByUsername(String username);
    UserResponse updateUser(String username, UpdateUserRequest request);
}

// UserManagementService.java - admin operations
public interface UserManagementService {
    void deleteUser(String username);
}

// UserValidationService.java - validation tách riêng
public interface UserValidationService {
    void validateUserUpdate(String username, UpdateUserRequest request);
    void validateEmailUnique(String email, String currentUsername);
}
```

**File ảnh hưởng:**
- `user-service/service/UserProfileService.java` (new)
- `user-service/service/UserManagementService.java` (rewrite, chỉ giữ deleteUser)
- `user-service/service/UserValidationService.java` (new)
- `user-service/service/impl/UserServiceImpl.java` (new, implements 2 interface)
- `user-service/service/impl/UserValidationServiceImpl.java` (new)
- `user-service/service/UserService.java` (delete)
- `user-service/repository/UserRepository.java` (thêm `findByEmail`, `existsByEmail`)
- `user-service/controller/UserController.java` (inject 2 interface riêng)

---

## 🎯 Đề xuất 3: Strategy Pattern cho authentication

### Vi phạm hiện tại

`AuthService.login()` hardcode 1 cách duy nhất (username/password). Nếu sau này thêm OAuth/SAML/2FA → phải sửa code cũ → vi phạm Open/Closed Principle.

### Đề xuất sửa

Theo PDF trang 50, áp dụng Strategy Pattern:

```java
// strategy/AuthenticationStrategy.java
public interface AuthenticationStrategy {
    boolean supports(String authenticationType);
    AuthResponse authenticate(LoginRequest request);
}

// strategy/PasswordAuthenticationStrategy.java
@Component
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {
    public boolean supports(String type) { return type == null || "password".equals(type); }
    public AuthResponse authenticate(LoginRequest req) { /* logic hiện tại */ }
}

// AuthServiceImpl dùng Spring inject TẤT CẢ strategy
@Service
public class AuthServiceImpl implements AuthenticationService {
    private final List<AuthenticationStrategy> strategies;

    public AuthResponse login(LoginRequest request) {
        return strategies.stream()
                .filter(s -> s.supports(request.getAuthType()))
                .findFirst()
                .orElseThrow(...)
                .authenticate(request);
    }
}
```

**Lợi ích Open/Closed:** Thêm OAuth = thêm class mới, KHÔNG sửa `AuthServiceImpl`.

**File ảnh hưởng:**
- `auth-service/strategy/AuthenticationStrategy.java` (new)
- `auth-service/strategy/PasswordAuthenticationStrategy.java` (new)
- `auth-service/dto/LoginRequest.java` (thêm field `authType`, optional)

**Lưu ý:** Hiện chỉ implement Password strategy. Khi cần thêm OAuth/2FA sau, chỉ thêm class mới.

---

## 🎯 Đề xuất 4: `ApiResponse` wrapper + `ResponseBuilder`

### Vi phạm hiện tại

Controllers trả response inconsistent:
```java
return ResponseEntity.ok("User registered successfully");        // String thuần
return ResponseEntity.ok(authService.login(request));            // Object AuthResponse
return ResponseEntity.ok(userManagementService.getAllUsers());   // List
```

Frontend phải code 3 cách parse khác nhau, không có timestamp/success flag để debug.

### Đề xuất sửa

Theo PDF trang 44-45, tạo wrapper consistent:

```java
// core/response/ApiResponse.java
@Data @Builder @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {...}
    public static <T> ApiResponse<T> error(String message) {...}
}

// core/response/ResponseBuilder.java
@Component
public class ResponseBuilder {
    public <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T data, String message) {...}
    public <T> ResponseEntity<ApiResponse<T>> buildCreatedResponse(T data, String message) {...}
    public <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(String message) {...}
    // + buildNotFound, buildUnauthorized, buildForbidden, buildConflict, buildNoContent
}

// Controller dùng:
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    return responseBuilder.buildSuccessResponse(
            authenticationService.login(request),
            "Login successful"
    );
}
```

**Response format thống nhất:**
```json
{
  "success": true,
  "data": {...},
  "message": "...",
  "timestamp": "2026-05-26T15:30:00"
}
```

**File ảnh hưởng:**
- `core/response/ApiResponse.java` (new)
- `core/response/ResponseBuilder.java` (new)
- `auth-service/controller/AuthController.java` (refactor dùng ResponseBuilder)
- `user-service/controller/UserController.java` (refactor dùng ResponseBuilder)

---

## 🎯 Đề xuất 5: Domain exceptions

### Vi phạm hiện tại

```java
throw new IllegalArgumentException("Username already exists: " + username);
```

Generic Java exception → HTTP 400 Bad Request (sai semantic, đúng phải là 409 Conflict).

### Đề xuất sửa

Tạo domain-specific exceptions trong `core/exception/`:
```java
public class BusinessException extends RuntimeException {...}
public class DuplicateUsernameException extends BusinessException {...}
public class DuplicateEmailException extends BusinessException {...}
```

Update `GlobalExceptionHandler` map sang HTTP 409:
```java
@ExceptionHandler({DuplicateUsernameException.class, DuplicateEmailException.class})
public ResponseEntity<ApiResponse<Void>> handleDuplicate(BusinessException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)   // ← 409 đúng semantic
            .body(ApiResponse.error(ex.getMessage()));
}
```

**File ảnh hưởng:**
- `core/exception/BusinessException.java` (new)
- `core/exception/DuplicateUsernameException.java` (new)
- `core/exception/DuplicateEmailException.java` (new)
- `core/exception/GlobalExceptionHandler.java` (update)
- `auth-service/exception/AuthExceptionHandler.java` (update theo ApiResponse format)

---

## 🎯 Đề xuất 6: Test improvements (@Nested + TestDataBuilder)

### Vi phạm hiện tại

Test class flat với 30 method, magic strings lặp lại nhiều lần:
```java
when(userRepository.findByUsername("newuser"))...   // magic string
when(passwordEncoder.encode("123456"))...           // magic string
```

### Đề xuất sửa

Theo PDF trang 53-54:

```java
@DisplayName("Auth services - SOLID refactored")
class AuthServiceTest {

    @Nested
    @DisplayName("Registration flow")
    class RegistrationTests {...}

    @Nested
    @DisplayName("Authentication flow (Strategy Pattern)")
    class AuthenticationTests {...}

    @Nested
    @DisplayName("Role management (admin)")
    class RoleManagementTests {...}
}

// testdata/TestDataBuilder.java - Builder Pattern
public final class TestDataBuilder {
    public static final String TEST_USERNAME = "newuser";
    public static final String TEST_PASSWORD = "123456";

    public static RegisterRequest aRegisterRequest() {...}
    public static User aUser() {...}
    public static User aUserWithRole(Role role) {...}
}
```

**File ảnh hưởng:**
- `auth-service/test/testdata/TestDataBuilder.java` (new)
- `user-service/test/testdata/TestDataBuilder.java` (new)
- `auth-service/test/service/AuthServiceTest.java` (rewrite với @Nested)
- `user-service/test/service/UserServiceTest.java` (rewrite với @Nested)
- `auth-service/test/integration/AuthServiceIntegrationTest.java` (update inject RegistrationService thay vì AuthService)
- `user-service/test/integration/UserServiceIntegrationTest.java` (update inject 2 interface riêng)
- `auth-service/test/controller/AuthControllerTest.java` (update inject 3 interface)
- `user-service/test/controller/UserControllerTest.java` (update inject 2 interface)
- `auth-service/test/exception/AuthExceptionHandlerTest.java` (update theo ApiResponse)

---

## 📊 Tổng kết file ảnh hưởng

| Loại | Số lượng |
|---|---|
| File mới sẽ tạo | 12 |
| File hiện có sẽ sửa | 10 |
| File hiện có sẽ xóa | 2 (UserService.java, AuthService.java cũ) |
| **Tổng files đụng đến** | **24** |

---

## ⚠️ Trade-off em nhận thấy

| (+) Lợi ích | (-) Trade-off |
|---|---|
| ✅ Tuân thủ SOLID đúng yêu cầu PDF | ❌ Số file tăng (12 file mới) |
| ✅ Code dễ test, dễ mở rộng (OCP) | ❌ Onboarding dev mới phức tạp hơn |
| ✅ Response format consistent toàn hệ thống | ❌ Phải refactor controllers + tests |
| ✅ Domain exception đúng semantic HTTP | ❌ Phải update GlobalExceptionHandler |
| ✅ Open/Closed: thêm OAuth không sửa code cũ | ❌ Strategy Pattern hơi over-engineering nếu chỉ có 1 cách auth |

---

## 🎯 Câu hỏi em xin thầy xác nhận

1. **Phạm vi 5 nhóm thay đổi trên** có hợp lý không? Có nhóm nào thầy thấy không cần thiết cho OJT project?

2. **Strategy Pattern** PDF chỉ "có thể" sử dụng (không bắt buộc). Em có nên implement đầy đủ (như đề xuất) hay skip vì chỉ có 1 strategy (password)?

3. **HTTP 409 Conflict** cho duplicate username thay vì 400 Bad Request — thầy có OK không? Em đọc RFC nói 409 đúng hơn cho duplicate resource.

4. **`@Nested` + `TestDataBuilder`** trong test — thầy có yêu cầu format khác không?

5. **Có thay đổi nào em đề xuất KHÔNG nên làm** trong scope OJT này?

---

## 📅 Kế hoạch thực hiện sau khi thầy duyệt

| Bước | Thời gian dự kiến |
|---|---|
| Tạo file mới (12 file) | ~2 giờ |
| Refactor file hiện có (10 file) | ~2 giờ |
| Update tests + verify pass | ~1 giờ |
| Run local test (mvn test) | ~30 phút |
| Run Postman E2E test | ~30 phút |
| Commit + push lên branch riêng | ~15 phút |
| **Tổng** | **~6 giờ** |

---

## 🙏 Lời nhắn

Em đã review kỹ PDF Phần 10 và identify được các vi phạm cụ thể. Trước khi tiến hành refactor (đụng đến 24 file), em xin thầy:

1. Xem qua đề xuất này
2. Confirm/correct các điểm cần thay đổi
3. Cho phép tiến hành refactor

Em sẽ chỉ commit code sau khi nhận được hướng dẫn từ thầy. Cảm ơn thầy.

---

**Author:** Nguyễn Hiếu An
**Date:** May 26, 2026
**Status:** ⏳ Awaiting teacher's approval
