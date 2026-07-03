# User Access Management System (UAM)

> Hệ thống quản lý người dùng theo kiến trúc **Microservices** — xây dựng với Java 17, Spring Boot 3, PostgreSQL, Kafka, Docker và CI/CD hoàn chỉnh.
>
> **Tác giả:** Nguyễn Hiếu An — Dự án On-the-Job Training (OJT) tại R2S Academy | FPT
> **Thời gian thực hiện:** 03/2026 – 06/2026

---

## 📌 Giới thiệu

UAM là hệ thống backend quản lý người dùng gồm **2 microservices độc lập** giao tiếp qua **Apache Kafka** (event-driven), bảo mật bằng **JWT + RBAC**, được **Docker hóa toàn bộ** và có **CI/CD pipeline tự động** build → test → deploy lên Docker Hub.

Dự án được phát triển theo quy trình chuyên nghiệp: **proposal → code review → refactor → verify**, trải qua nhiều vòng review production-readiness (tài liệu đầy đủ tại [`docs/review/`](docs/review/)).

---

## 🏗 Kiến trúc hệ thống

```
                                  ┌─────────────────┐
                     ┌───────────▶│   PostgreSQL    │◀───────────┐
                     │            │  (2 databases)  │            │
                     │            └─────────────────┘            │
              ┌──────┴───────┐                          ┌────────┴──────┐
   :8081 ────▶│ auth-service │       Kafka events       │ user-service  │◀──── :8082
              │  (JWT, RBAC) │─────▶ user-registered ──▶│   (profile,   │
              │              │◀──── user-deleted  ◀─────│    admin)     │
              └──────┬───────┘                          └────────┬──────┘
                     │            ┌─────────────────┐            │
                     └───────────▶│   Prometheus    │◀───────────┘
                                  │  (:9090 scrape  │
                                  │   /actuator)    │
                                  └─────────────────┘
```

### Maven multi-module

| Module | Vai trò |
|---|---|
| **`core`** | Thư viện dùng chung: `ApiResponse` wrapper, `ResponseBuilder`, `ApiResponseWriter`, domain exceptions, `GlobalExceptionHandler`, `JwtUtil`, `SecurityConstants`, Kafka events |
| **`auth-service`** (:8081) | Đăng ký, đăng nhập (JWT), phân quyền role — tổ chức package theo chức năng: `authentication/`, `registration/`, `password/`, `role/` + Strategy Pattern |
| **`user-service`** (:8082) | Quản lý profile, thao tác admin — packages: `profile/`, `management/`, `validation/` |

---

## 🛠 Tech Stack

| Nhóm | Công nghệ |
|---|---|
| **Ngôn ngữ / Framework** | Java 17, Spring Boot 3.4.4, Spring Security, Spring Data JPA |
| **Bảo mật** | JWT (jjwt), BCrypt, RBAC (`@PreAuthorize`), Rate Limiting (Bucket4j) |
| **Database** | PostgreSQL, Flyway migration (versioned, data-preserving) |
| **Messaging** | Apache Kafka + Zookeeper (event-driven giữa 2 services) |
| **DevOps** | Docker (multi-stage build, non-root user), Docker Compose, GitLab CI/CD |
| **Monitoring** | Spring Boot Actuator, Prometheus, Logback (log theo profile) |
| **Testing** | JUnit 5, Mockito, MockMvc, EmbeddedKafka, H2, Postman/Newman, JaCoCo |

---

## ✨ Tính năng chính

- ✅ **Đăng ký / Đăng nhập** trả về JWT token
- ✅ **RBAC** 3 role: `ADMIN`, `USER`, `MODERATOR` — bảo vệ endpoint bằng `@PreAuthorize`
- ✅ **Admin gán role** cho user (chặn admin tự đổi role chính mình)
- ✅ **Quản lý profile**: xem / cập nhật thông tin cá nhân
- ✅ **Đồng bộ dữ liệu giữa 2 service qua Kafka** (đăng ký ở auth-service → tự tạo profile ở user-service)
- ✅ **Rate limiting** login: 5 request/phút/IP, TTL eviction, hỗ trợ `X-Forwarded-For` với trusted proxy
- ✅ **Response format thống nhất** toàn hệ thống (kể cả lỗi từ security filter):
  ```json
  { "success": true, "data": {...}, "message": "...", "timestamp": "..." }
  ```
- ✅ **HTTP status đúng semantic**: 201 Created (register), 409 Conflict (duplicate), 401/403 phân biệt rõ
- ✅ **Multi-profile config**: `dev` / `docker` / `test` / `ci` / `prod` — production **fail-fast** (không có fallback secret)
- ✅ **Health check + metrics**: `/actuator/health`, `/actuator/prometheus`

---

## 🧠 Điểm nhấn kỹ thuật (Engineering Highlights)

### 1. SOLID Principles & Design Patterns
- **ISP/SRP**: tách "fat interface" thành các interface chuyên trách (auth: 4 interfaces, user: 3 interfaces), mỗi package = 1 chức năng
- **OCP + Strategy Pattern**: `AuthenticationStrategy` — thêm cách login mới (OAuth/2FA) chỉ cần thêm class, không sửa code cũ
- **DIP**: service phụ thuộc abstraction (`PasswordService`) thay vì concrete (`BCryptPasswordEncoder`)
- **Patterns khác**: Repository (Spring Data JPA), Observer (Kafka), Builder (`ApiResponse`, `TestDataBuilder`), Factory Method
- **Centralized exception handling**: 1 `@RestControllerAdvice` duy nhất ở `core` làm source of truth

### 2. Testing Pyramid — 96 automated tests + 18 Postman E2E
| Tầng | Số lượng | Công cụ |
|---|---|---|
| Unit tests | ~75 | JUnit 5 + Mockito (`@Nested`, AAA+, ArgumentCaptor, TestDataBuilder) |
| Integration tests | 4 | `@SpringBootTest` + H2 (mode PostgreSQL) |
| E2E tests (Java) | 2 | `TestRestTemplate` + EmbeddedKafka |
| E2E tests (Postman) | 18 | 2 collections tại [`postman/`](postman/) — auto random data, token chaining |

- **JaCoCo coverage gate ≥ 85%** (LINE) — enforce trong build, fail pipeline nếu không đạt

### 3. CI/CD Pipeline (GitLab)
```
push → [build: mvn install] → [test: 96 tests + coverage gate] → [deploy: docker build --no-cache → push Docker Hub]
```
- Deploy chỉ chạy trên `main`, image build với `--no-cache --pull` đảm bảo fresh
- Đã xử lý các vấn đề thực tế: Docker layer cache, Flyway checksum mismatch, Kafka-Zookeeper race condition, volume persistence — ghi lại đầy đủ trong [`docs/CICD_Troubleshooting_Journey.md`](docs/CICD_Troubleshooting_Journey.md)

### 4. Production Readiness
- Production profile **fail-fast**: thiếu env (`JWT_SECRET`, DB credentials) → app từ chối start, không fallback về default không an toàn
- Flyway migration **bảo toàn dữ liệu** (`ALTER TABLE` + backfill, không `DROP TABLE`)
- Dockerfile multi-stage, chạy **non-root user**
- Rate limit hardening: TTL eviction chống memory leak, trusted-proxy chống fake `X-Forwarded-For`

---

## 🚀 Chạy dự án

### Cách 1 — Docker Compose (nhanh nhất)

```bash
git clone <repo-url>
cd user-access-management
docker compose up -d
```

> Nếu Kafka lỗi khi khởi động lần đầu (race condition với Zookeeper), chạy tuần tự:
> ```powershell
> .\scripts\start-infra.ps1   # start zookeeper → kafka → postgres tuần tự
> docker compose up -d        # start phần còn lại
> ```

### Cách 2 — Dev local (code mới nhất)

```powershell
# Terminal 1: infrastructure
.\scripts\start-infra.ps1

# Terminal 2 & 3: services (tự set profile dev)
.\scripts\run-auth-service.ps1
.\scripts\run-user-service.ps1
```

### Verify

```bash
curl http://localhost:8081/actuator/health   # {"status":"UP"}
curl http://localhost:8082/actuator/health   # {"status":"UP"}
```

### Chạy tests

```bash
mvn clean install          # build + 96 tests + coverage gate
# Windows local (EmbeddedKafka không hỗ trợ loopback):
mvn test -Dtest='!AuthFlowE2ETest' -Dsurefire.failIfNoSpecifiedTests=false
```

### E2E với Postman

Import 3 file trong [`postman/`](postman/) → chọn environment `UAM E2E Environment` → Run collection. Chi tiết: [`postman/README.md`](postman/README.md)

---

## 📡 API chính

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/auth/register` | Public | Đăng ký → 201 + JWT |
| POST | `/auth/login` | Public | Đăng nhập → 200 + JWT (rate limited) |
| POST | `/auth/admin/assign-role` | ADMIN | Gán role cho user |
| GET | `/users/me` | Authenticated | Xem profile cá nhân |
| PUT | `/users/me` | Authenticated | Cập nhật profile |
| GET | `/users` | ADMIN | Danh sách toàn bộ user |
| DELETE | `/users/{username}` | ADMIN | Xóa user (publish Kafka event) |
| GET | `/actuator/health` | Public | Health check |
| GET | `/actuator/prometheus` | Public* | Metrics cho Prometheus (*nội bộ network) |

---

## 📂 Cấu trúc thư mục

```
user-access-management/
├── core/                    # Module dùng chung (response, exception, security, events)
├── auth-service/            # Service xác thực (:8081)
│   └── src/main/java/com/r2s/auth/
│       ├── service/{authentication,registration,password,role}/
│       ├── strategy/        # Strategy Pattern cho authentication
│       ├── security/        # JwtFilter, RateLimitFilter, custom handlers
│       └── ...
├── user-service/            # Service quản lý user (:8082)
│   └── src/main/java/com/r2s/user/
│       ├── service/{profile,management,validation}/
│       └── ...
├── docs/                    # Tài liệu: review reports, testing guide, CI/CD journey
├── postman/                 # E2E test collections + hướng dẫn
├── scripts/                 # PowerShell scripts chạy local
├── prometheus/              # Config Prometheus scrape
├── docker-compose.yaml
└── .gitlab-ci.yml           # Pipeline build → test → deploy
```

---

## 📚 Tài liệu chi tiết

| Tài liệu | Nội dung |
|---|---|
| [`docs/Testing_Guide.md`](docs/Testing_Guide.md) | Pyramid of Tests (Unit/Integration/E2E) — What/Why/How |
| [`docs/CICD_Troubleshooting_Journey.md`](docs/CICD_Troubleshooting_Journey.md) | 5 vấn đề CI/CD thực tế đã gặp & cách xử lý |
| [`docs/review/`](docs/review/) | Các vòng code review + fix reports (quy trình proposal → review → refactor) |
| [`postman/README.md`](postman/README.md) | Hướng dẫn chạy 18 E2E tests |

---

## 🎓 Kỹ năng thể hiện qua dự án

- Thiết kế & triển khai **microservices** với event-driven architecture (Kafka)
- Bảo mật ứng dụng: **JWT, RBAC, BCrypt, rate limiting**, security filter chain
- Áp dụng **SOLID & Design Patterns** vào refactor có kiểm soát (review-driven)
- Viết **automated tests đa tầng** (unit → integration → E2E) với coverage gate
- Xây dựng **CI/CD pipeline** hoàn chỉnh + troubleshoot vấn đề deploy thực tế
- Quản lý **database migration** an toàn với Flyway
- **Docker hóa** ứng dụng theo best practices (multi-stage, non-root)
- Làm việc theo quy trình chuyên nghiệp: **proposal → code review → fix → verify**

---

**Nguyễn Hiếu An** — OJT Java Backend @ R2S Academy | FPT
📧 hieuannguyen2k6@gmail.com
