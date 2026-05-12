# 📚 Testing Guide - User Access Management Project

> Tài liệu giải thích về Unit Test, Integration Test, và E2E Test theo định dạng **What / Why / How**.
>
> **Sinh viên:** Nguyễn Hiếu An | FPT HCMC | OJT R2S
> **Dự án:** User Access Management System (UAM)

---

## 📑 Mục lục

1. [Tổng quan: Pyramid of Tests](#1-tổng-quan-pyramid-of-tests)
2. [Unit Test](#2-unit-test)
3. [Integration Test](#3-integration-test)
4. [E2E Test](#4-e2e-test)
5. [So sánh 3 loại Test](#5-so-sánh-3-loại-test)
6. [CI/CD Verification](#6-cicd-verification)
7. [Các công cụ hỗ trợ](#7-các-công-cụ-hỗ-trợ)

---

## 1. Tổng quan: Pyramid of Tests

### What (Là gì?)

Pyramid of Tests là **mô hình kiến trúc test** phổ biến trong industry, chia tests thành 3 tầng theo tỉ lệ giảm dần:

```
         /\
        /E2E\           ← Ít nhất, chậm nhất, đắt nhất
       /------\
      /  IT    \        ← Vừa phải
     /----------\
    /   UNIT     \      ← Nhiều nhất, nhanh nhất, rẻ nhất
   /--------------\
```

### Why (Tại sao có pyramid?)

- **Test pyramid cân bằng** giữa **độ tin cậy** (E2E test giống production) và **tốc độ** (Unit test chạy nhanh)
- Nếu chỉ làm E2E test → chạy chậm, fragile, khó debug
- Nếu chỉ làm Unit test → có thể miss bug ở ranh giới giữa các tầng (DB, HTTP, security)
- **Pyramid = best practice của industry**: nhiều Unit, ít E2E

### How (Áp dụng trong dự án này?)

| Tầng | Loại | Số lượng | Mục đích |
|------|------|----------|----------|
| Đáy | Unit Test | 47 tests | Test từng method/class với mock |
| Giữa | Integration Test | 4 tests | Test Service + Repository + DB thật |
| Đỉnh | E2E Test (Java) | 2 tests | Test full flow qua HTTP với Embedded Kafka |
| Đỉnh | E2E Test (Postman) | 6 requests | Test user journey thật |

**Tổng: 59 automated tests + 6 Postman requests = 65 tests**

---

## 2. Unit Test

### 🎯 What (Là gì?)

Unit Test là test **đơn vị code nhỏ nhất** (thường là 1 method hoặc 1 class) với toàn bộ dependency được **mock**.

**Đặc điểm:**
- Test 1 logic độc lập, không phụ thuộc bên ngoài
- Không cần DB, Kafka, network, file system
- Chạy nhanh (millisecond/test)
- Cô lập hoàn toàn

### 💡 Why (Tại sao cần?)

- **Tốc độ:** Hàng trăm test chạy trong vài giây → feedback nhanh khi code thay đổi
- **Cô lập lỗi:** Nếu fail, biết ngay lỗi ở method nào (không bị nhiễu bởi DB/Kafka)
- **Coverage cao:** Dễ cover các edge case (null, empty, exception)
- **Refactor an toàn:** Đổi code mà tests vẫn pass = code mới vẫn đúng
- **Tài liệu sống:** Đọc test → hiểu method làm gì, edge case nào quan trọng

### 🛠️ How (Làm thế nào?)

#### Công cụ
- **JUnit 5** - framework test
- **Mockito** - tạo mock object
- **AssertJ / JUnit Assertions** - viết assertion

#### Cấu trúc AAA+ Pattern

```java
@Test
@DisplayName("TC001 - Register: Happy case - new username should succeed")
void register_HappyCase_ReturnsToken() {
    // ARRANGE: chuẩn bị data + mock behavior
    when(userRepository.findByUsername("newuser"))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("123456"))
        .thenReturn("encodedPassword");
    when(userRepository.save(any(User.class)))
        .thenReturn(savedUser);
    when(jwtUtil.generateToken("newuser"))
        .thenReturn("mockedToken");

    // ACT: gọi method cần test
    AuthResponse response = authService.register(registerRequest);

    // ASSERT: kiểm tra output
    assertNotNull(response);
    assertEquals("mockedToken", response.getToken());

    // VERIFY: kiểm tra side effect (mock có được gọi đúng không)
    verify(userRepository).save(any(User.class));
}
```

#### Naming convention

Format: `action_expected_condition()`

Ví dụ:
- `register_HappyCase_ReturnsToken` — happy path
- `register_WhenDuplicateUsername_ThrowsException` — error path
- `login_WhenWrongPassword_ThrowsException` — error path
- `getUserByUsername_WhenNotFound_ThrowsException` — error path

→ Nhìn tên test biết test gì, không cần đọc body.

#### Loại test cases

1. **Happy Case** - Input đúng, expect kết quả OK
   ```java
   @Test void register_HappyCase_ReturnsToken() { ... }
   ```

2. **Worst Case** - Input gây lỗi, expect exception
   ```java
   @Test void register_WhenDuplicateUsername_ThrowsException() { ... }
   ```

3. **Edge Case** - Input biên (null, empty, very long)
   ```java
   @Test void register_WhenUsernameEmpty_ThrowsException() { ... }
   ```

4. **Security Case** - Test bảo mật
   ```java
   @Test void doFilterInternal_ExpiredToken_Returns401() { ... }
   ```

#### ArgumentCaptor & verifyNoMoreInteractions (nâng cao)

```java
@Test
void register_HappyCase_VerifyDetails() {
    // ... arrange + act ...

    // ArgumentCaptor: capture object truyền vào mock
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    
    User savedUser = userCaptor.getValue();
    assertEquals("newuser", savedUser.getUsername());
    assertEquals("encodedPassword", savedUser.getPassword());  // verify password đã encode
    assertEquals(Role.ROLE_USER, savedUser.getRole());          // verify role mặc định
    
    // verifyNoMoreInteractions: đảm bảo mock không bị gọi thêm gì khác
    verifyNoMoreInteractions(userEventProducer);
}
```

→ Test kỹ hơn, bắt được bug nếu code lỡ thêm side effect.

#### Trong dự án này

**File:** `auth-service/src/test/java/com/r2s/auth/service/AuthServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;          // FAKE
    @Mock PasswordEncoder passwordEncoder;        // FAKE
    @Mock JwtUtil jwtUtil;                        // FAKE
    @Mock UserEventProducer userEventProducer;    // FAKE
    
    @InjectMocks AuthService authService;         // REAL (đang test)
    
    // 18 tests covering happy/worst/edge/security cases
}
```

**Tổng 47 Unit Tests** trong dự án:
- AuthServiceTest: 18 tests
- UserServiceTest: 18 tests
- AuthControllerTest: 4 tests
- UserControllerTest: 2 tests
- JwtFilterTest: 4 tests
- KafkaProducer/ConsumerTest: 6 tests
- AuthExceptionHandlerTest: 5 tests
- ModeratorControllerTest: 1 test

---

## 3. Integration Test

### 🎯 What (Là gì?)

Integration Test là test **tích hợp giữa nhiều thành phần** (Service + Repository + DB) hoạt động cùng nhau, **không mock** các thành phần này.

**Đặc điểm:**
- Load Spring Application Context (`@SpringBootTest`)
- DB thật (H2 in-memory hoặc Testcontainers)
- Chạy chậm hơn Unit test (vài giây/test)
- Vẫn không gọi qua HTTP (gọi method trực tiếp)

### 💡 Why (Tại sao cần?)

Unit test pass không có nghĩa là code chạy được trong thực tế. Integration test phát hiện:

- **SQL/JPA mapping sai:** Entity không map đúng cột DB
- **Transaction boundary sai:** `@Transactional` không hoạt động đúng
- **Cascade issue:** Save parent nhưng không save children
- **Repository query sai:** `findByUsername` viết sai
- **DB constraint:** Unique key, foreign key constraint trigger lỗi
- **Spring DI sai:** Bean inject sai, missing config

→ Tóm lại: **Verify rằng các tầng phối hợp đúng**, không chỉ logic riêng lẻ.

### 🛠️ How (Làm thế nào?)

#### Công cụ
- **Spring Boot Test** (`@SpringBootTest`)
- **H2 Database** - DB in-memory mode PostgreSQL
- **Testcontainers** - DB thật trong Docker container (tối ưu hơn nhưng cần Docker)

#### Setup application-test.properties

```properties
# Dùng H2 với mode PostgreSQL để mô phỏng behavior thật
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false

# Mock Kafka (vì IT không test Kafka)
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
```

#### Cấu trúc Integration Test

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Test: AuthService + Repository + DB")
class AuthServiceIntegrationTest {

    @Autowired AuthService authService;          // REAL
    @Autowired UserRepository userRepository;    // REAL (connect H2)
    
    @MockBean
    KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate; // Vẫn mock Kafka

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean DB trước mỗi test
    }

    @Test
    @DisplayName("IT001 - Register saves user to DB")
    void register_SavesToDbAndCanBeFound() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest("integration_user", "password123");

        // ACT
        AuthResponse response = authService.register(request);

        // ASSERT - response
        assertNotNull(response);
        assertNotNull(response.getToken());

        // ASSERT - DB thật
        Optional<User> savedUser = userRepository.findByUsername("integration_user");
        assertTrue(savedUser.isPresent());
        assertEquals(Role.ROLE_USER, savedUser.get().getRole());
        assertNotEquals("password123", savedUser.get().getPassword()); // verify password đã encode
    }

    @Test
    @DisplayName("IT002 - Duplicate username throws exception")
    void register_WhenDuplicateUsername_ThrowsException() {
        // ARRANGE - tạo user đầu tiên
        authService.register(new RegisterRequest("duplicate_user", "password123"));

        // ACT + ASSERT
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> authService.register(new RegisterRequest("duplicate_user", "another"))
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }
}
```

#### Trong dự án này

**File:**
- `auth-service/src/test/java/com/r2s/auth/integration/AuthServiceIntegrationTest.java`
- `user-service/src/test/java/com/r2s/user/integration/UserServiceIntegrationTest.java`

**4 Integration Tests:**

| ID | Test | Mục đích |
|----|------|----------|
| IT001 | `register_SavesToDbAndCanBeFound` | Register → user save vào DB → query lại tìm thấy |
| IT002 | `register_WhenDuplicateUsername_ThrowsException` | Duplicate → throw exception, DB không có duplicate |
| IT003 | `updateUser_UpdatesDbAndPersists` | Update user → query lại verify giá trị mới |
| IT004 | `deleteUser_RemovesFromDb` | Delete user → query lại không tìm thấy |

#### ⚠️ Lưu ý về Testcontainers

Em đã thử setup Testcontainers + PostgreSQL nhưng gặp lỗi với Docker Desktop 29.4.0 (mới release 2026). Testcontainers 1.21.3 chưa support đầy đủ → fallback về H2 mode PostgreSQL.

Khi có version tương thích, sẽ migrate sang Testcontainers vì:
- DB thật (PostgreSQL) thay vì H2 (có khác biệt nhỏ)
- Verify SQL query với syntax PostgreSQL thật
- Migration scripts (Flyway) chạy thật

---

## 4. E2E Test

### 🎯 What (Là gì?)

E2E (End-to-End) Test là test **toàn trình từ HTTP request đến response**, mô phỏng một user thật đang dùng app.

**Đặc điểm:**
- Start full Spring application
- Gọi qua HTTP thật (TestRestTemplate hoặc Postman)
- Có đầy đủ: Security Filter, Controller, Service, Repository, DB, Kafka
- Chạy chậm nhất (>10s/test)
- Test "user journey" thay vì 1 method

### 💡 Why (Tại sao cần?)

Unit + Integration Test có thể pass nhưng app vẫn lỗi vì:

- **URL mapping sai:** Quên `@PostMapping("/auth/register")` → 404
- **JSON serialization:** Field không serialize đúng → response sai format
- **HTTP status code:** Throw exception nhưng GlobalExceptionHandler không bắt → trả 500 thay vì 400
- **Spring Security:** Filter chặn nhầm request → 401/403
- **CORS:** Bị block ở browser
- **Kafka end-to-end:** Producer publish nhưng Consumer không nhận

→ E2E là **last line of defense**, mô phỏng đúng cách user thật dùng app.

### 🛠️ How (Làm thế nào?)

#### Cách 1: Java E2E Test với @SpringBootTest

**Công cụ:**
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` - start app trên random port
- `TestRestTemplate` - HTTP client trong test
- `@EmbeddedKafka` - Kafka chạy trong RAM

**File:** `auth-service/src/test/java/com/r2s/auth/e2e/AuthFlowE2ETest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"user-registered-topic", "user-deleted-topic"})
class AuthFlowE2ETest {

    @LocalServerPort int port;
    
    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;

    @Test
    @DisplayName("TC057 - E2E: Register then Login returns valid token")
    void e2e_RegisterThenLogin_ReturnsValidToken() {
        // ARRANGE: random username
        String username = "e2e_" + System.currentTimeMillis();
        String password = "password123";

        // ACT 1: Register qua HTTP thật
        RegisterRequest registerRequest = new RegisterRequest(username, password);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/auth/register",
            registerRequest,
            AuthResponse.class
        );

        // ASSERT: Register OK
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody().getToken());

        // ASSERT: DB thật có user
        Optional<User> savedUser = userRepository.findByUsername(username);
        assertTrue(savedUser.isPresent());

        // ACT 2: Login qua HTTP thật
        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/auth/login",
            loginRequest,
            AuthResponse.class
        );

        // ASSERT: Login OK + token hợp lệ
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().getToken());
    }
}
```

**TC058:** Tạo admin → admin login → POST /auth/admin/assign-role → verify role đổi trong DB

#### Cách 2: Postman Collection

**Công cụ:**
- Postman Desktop App (GUI)
- Postman Collection Runner

**Setup:**

1. **Collection:** `postman/auth-e2e-all-in-one.postman_collection.json`
2. **Environment:** `postman/auth-env.postman_environment.json`

**Environment Variables:**

| Variable | Value |
|----------|-------|
| `baseUrl` | `http://localhost:8081/auth` |
| `userBaseUrl` | `http://localhost:8082` |
| `password` | `123456` |
| `username` | (auto-generate qua pre-request script) |
| `token` | (auto-save sau login) |

**Pre-request Script** (tạo username random mỗi lần chạy):
```javascript
const randomSuffix = Math.floor(Math.random() * 100000);
pm.environment.set('username', 'e2euser_' + randomSuffix);
```

**Test Script** (auto-save token):
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});
pm.test("Response contains token", function () {
    const json = pm.response.json();
    pm.expect(json.token).to.be.a('string');
    pm.environment.set('token', json.token);
});
```

**6 Test Cases:**

| # | Test | Method | Endpoint | Expected | Status |
|---|------|--------|----------|----------|--------|
| 1 | Register | POST | /auth/register | 200 | ✅ PASS |
| 2 | Login & Save Token | POST | /auth/login | 200 | ✅ PASS |
| 3 | Get Profile | GET | /users/me | 200 | ✅ PASS |
| 4 | Negative: Wrong password | POST | /auth/login | 401 | ✅ PASS |
| 5 | Negative: No token | GET | /users/me | 403 | ✅ PASS |
| 6 | Negative: Duplicate username | POST | /auth/register | 400 | ✅ PASS |

**Cách chạy:**
1. Start Docker: `docker compose up -d postgres-db kafka zookeeper`
2. Start auth-service: `mvn spring-boot:run -pl auth-service`
3. Start user-service: `mvn spring-boot:run -pl user-service`
4. Mở Postman → Import Collection + Environment
5. Click **Run** → **Run Auth E2E All-in-One**

---

## 5. So sánh 3 loại Test

| Tiêu chí | Unit Test | Integration Test | E2E Test |
|----------|-----------|------------------|----------|
| **Phạm vi** | 1 method/class | Service + Repo + DB | Full flow (HTTP → DB → Kafka) |
| **Mock** | Toàn bộ dependency | Chỉ mock external (Kafka) | Không mock gì (hoặc Embedded) |
| **DB** | Không cần | H2 in-memory | DB thật (H2/Testcontainers) |
| **HTTP** | Không qua HTTP | Không qua HTTP | Qua HTTP thật |
| **Spring Context** | Không load | Load đầy đủ | Load đầy đủ + web server |
| **Tốc độ** | ⚡ Mili giây | 🐢 Vài giây | 🐌 >10 giây |
| **Annotation** | `@Mock` + `@InjectMocks` | `@SpringBootTest` + `@ActiveProfiles("test")` | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@EmbeddedKafka` |
| **HTTP Client** | N/A | N/A | TestRestTemplate / Postman |
| **Số lượng (best practice)** | Nhiều nhất | Vừa | Ít |
| **Trong dự án này** | 47 tests | 4 tests | 2 Java + 6 Postman |

### Khi nào dùng loại nào?

- **Logic phức tạp, nhiều branch** → Unit Test (cover edge cases nhanh)
- **Query SQL/JPA, transaction** → Integration Test (verify DB layer)
- **User journey (register → login → profile)** → E2E Test (verify toàn flow)
- **Security flow (JWT, role)** → E2E Test + Unit Test cho filter

---

## 6. CI/CD Verification

### How to verify mỗi stage thành công?

#### Stage 1: BUILD

**Local verify:**
```bash
mvn clean install -DskipTests
```

Output thành công:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  XX s
```

**Trên GitLab CI:**
- Vào tab **CI/CD → Pipelines**
- Tìm pipeline mới nhất
- Stage `build` có icon ✅ xanh
- Click vào job để xem log
- Artifacts có file `.jar` (target/*.jar)

#### Stage 2: TEST

**Local verify:**
```bash
mvn clean verify
```

Output thành công:
```
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

**Trên GitLab CI:**
- Stage `test` có icon ✅
- Trong log có dòng `Tests run: XX, Failures: 0, Errors: 0`
- JaCoCo: `All coverage checks have been met`
- Có thể config coverage report hiển thị trên MR

**Mở Test Report HTML local:**
```bash
mvn surefire-report:report
start auth-service/target/reports/surefire.html
start user-service/target/reports/surefire.html
```

**Mở Coverage Report local:**
```bash
start auth-service/target/site/jacoco/index.html
start user-service/target/site/jacoco/index.html
```

#### Stage 3: DEPLOY

**Trên GitLab CI:**
- Stage `deploy` có icon ✅
- Log có dòng:
  ```
  $ docker push hhieuann/auth-service:latest
  The push refers to repository [docker.io/hhieuann/auth-service]
  ...
  latest: digest: sha256:... size: ...
  ```

**Verify trên Docker Hub:**
1. Mở https://hub.docker.com/u/hhieuann
2. Vào repository `auth-service` và `user-service`
3. Tab **Tags** → có tag `latest` với timestamp gần đây (vài phút trước)
4. Có thể pull về local test:
   ```bash
   docker pull hhieuann/auth-service:latest
   docker run -p 8081:8081 hhieuann/auth-service:latest
   ```

#### Pipeline status badges (nâng cao)

Thêm vào README.md:
```markdown
[![pipeline status](https://gitlab.com/hhieuann/user-access-management/badges/main/pipeline.svg)](https://gitlab.com/hhieuann/user-access-management/-/commits/main)
[![coverage report](https://gitlab.com/hhieuann/user-access-management/badges/main/coverage.svg)](https://gitlab.com/hhieuann/user-access-management/-/commits/main)
```

→ Badge tự động cập nhật mỗi lần pipeline chạy.

#### Email notification

GitLab tự động gửi email:
- ❌ Pipeline failed
- ✅ Pipeline successful (có thể tắt)

Vào Settings → Notifications để config.

#### Verify checklist nhanh

```
✅ git push xong
✅ Vào GitLab → CI/CD → Pipelines
✅ Pipeline mới chạy → đợi ~2-3 phút
✅ build ✅ test ✅ deploy
✅ Vào Docker Hub → có image mới
✅ docker pull về local → run thử
✅ curl http://localhost:8081/actuator/health → response { "status": "UP" }
```

---

## 7. Các công cụ hỗ trợ

### JaCoCo - Code Coverage Tool

**What:** Đo % code được test (Line, Branch, Method coverage).

**Why:** Đảm bảo code đủ test, bắt buộc ≥ 85% theo industry standard.

**How:** Config trong `pom.xml`, chạy `mvn verify` → report tại `target/site/jacoco/index.html`.

**Trong dự án:**
- Required: LINE coverage ≥ 85%
- Excluded: `dto/`, `entity/`, `config/`, `controller/`, `security/`, `Application*`, `exception/`
- Actual: auth-service ≥ 85%, user-service ≥ 85% ✅

### Mockito - Mock Framework

**What:** Tạo "fake object" để test mà không cần dependency thật.

**Why:** Cô lập unit, chạy nhanh, kiểm soát behavior.

**How:**
```java
@Mock UserRepository repo;
when(repo.findById(1L)).thenReturn(Optional.of(user));
verify(repo).save(any(User.class));
```

### Spring Boot Test

**What:** Framework test cho Spring Boot app.

**Why:** Test với Spring context đầy đủ (DI, AOP, transaction).

**How:**
```java
@SpringBootTest                    // Full context
@SpringBootTest(webEnvironment=RANDOM_PORT)  // E2E với web server
@DataJpaTest                       // Chỉ JPA layer
@WebMvcTest(AuthController.class)  // Chỉ Controller
```

### Postman + Newman

**Postman (GUI):** Tool gọi API qua HTTP, có Collection Runner.

**Newman (CLI):** Chạy Postman Collection từ command line, tích hợp CI/CD được.

**Why:** Test E2E từ góc độ client thật, share collection với team dễ dàng.

**How:**
```bash
# GUI: mở Postman → Import → Run
# CLI: 
newman run postman/auth-e2e-all-in-one.postman_collection.json \
    -e postman/auth-env.postman_environment.json
```

### Maven Surefire Plugin

**What:** Plugin chạy tests trong Maven, generate HTML report.

**Why:** Visualize kết quả test, dễ share.

**How:**
```bash
mvn surefire-report:report
# Output: target/reports/surefire.html
```

---

## 🎯 Tổng kết

| Tầng | Loại Test | Tốc độ | Độ tin cậy | Số lượng | Trong dự án |
|------|-----------|--------|------------|----------|-------------|
| Đáy | Unit | ⚡⚡⚡ | ⭐⭐ | Nhiều | 47 tests |
| Giữa | Integration | ⚡⚡ | ⭐⭐⭐ | Vừa | 4 tests |
| Đỉnh | E2E | ⚡ | ⭐⭐⭐⭐⭐ | Ít | 2+6 tests |

**Pyramid balanced = code quality cao + feedback nhanh.**

✅ Tests pass + Coverage ≥ 85% + CI/CD green = code ready for production

---

## 📚 Tài liệu tham khảo

- [Martin Fowler - Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Mockito Documentation](https://site.mockito.org/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Postman Learning Center](https://learning.postman.com/)

---

**Author:** Nguyễn Hiếu An  
**Project:** User Access Management System (UAM) - OJT R2S  
**Date:** May 2026
