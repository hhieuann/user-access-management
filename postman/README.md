# Postman E2E Tests — UAM Project

Bộ test E2E (End-to-End) qua Postman để chứng minh API thực sự hoạt động đúng từ góc nhìn client.

**Tổng:** 18 test cases / 2 collections

---

## 📁 Files trong folder này

| File | Mục đích |
|---|---|
| `auth-service-e2e.postman_collection.json` | 8 tests cho auth-service (register, login, role) |
| `user-service-e2e.postman_collection.json` | 10 tests cho user-service (profile, admin, actuator) |
| `uam-e2e.postman_environment.json` | Biến môi trường (baseUrl, token, username) |
| `README.md` | File này |

---

## 🚀 Cách chạy

### Bước 1: Khởi động services

Có 2 cách:

**Cách A — Docker Compose (recommended):**
```bash
# Từ thư mục root của project
docker compose up -d
```

Đợi 30–60 giây cho tất cả container start (postgres, kafka, zookeeper, auth-service, user-service, prometheus).

Kiểm tra:
```bash
docker ps
# Phải thấy 6 container running, status healthy
```

**Cách B — Chạy local từ IDE:**
```bash
# Terminal 1: PostgreSQL + Kafka
docker compose up -d postgres-db kafka zookeeper

# Terminal 2: auth-service
mvn spring-boot:run -pl auth-service

# Terminal 3: user-service
mvn spring-boot:run -pl user-service
```

### Bước 2: Verify services healthy

```bash
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP"}

curl http://localhost:8082/actuator/health
# Expected: {"status":"UP"}
```

### Bước 3: Import vào Postman

1. Mở **Postman Desktop App**
2. Click **Import** (góc trên trái) → chọn cả 3 file:
   - `auth-service-e2e.postman_collection.json`
   - `user-service-e2e.postman_collection.json`
   - `uam-e2e.postman_environment.json`
3. Ở dropdown environment (góc trên phải), chọn **"UAM E2E Environment"**

### Bước 4: Chạy collection

#### Cách A — Chạy cả collection (Collection Runner)

1. Click vào collection (vd `UAM - Auth Service E2E`)
2. Click nút **"Run"** (góc trên phải) → mở Collection Runner
3. Đảm bảo:
   - Environment = "UAM E2E Environment"
   - Tất cả requests được tick
4. Click **"Run UAM - Auth Service E2E"**
5. Xem kết quả: mỗi request có icon ✅ (PASS) hoặc ❌ (FAIL)

#### Cách B — Chạy từng test riêng

Click từng request → Send → xem tab "Test Results" ở response panel.

#### Cách C — Chạy qua CLI (Newman)

```bash
# Install Newman (1 lần)
npm install -g newman

# Chạy auth collection
newman run postman/auth-service-e2e.postman_collection.json \
    -e postman/uam-e2e.postman_environment.json

# Chạy user collection
newman run postman/user-service-e2e.postman_collection.json \
    -e postman/uam-e2e.postman_environment.json

# Export HTML report
newman run postman/auth-service-e2e.postman_collection.json \
    -e postman/uam-e2e.postman_environment.json \
    -r htmlextra \
    --reporter-htmlextra-export auth-e2e-report.html
```

---

## 📋 Danh sách test cases

### Auth Service E2E (8 tests)

| # | Test | Method | Endpoint | Expected | Verify |
|---|---|---|---|---|---|
| 1 | Register Happy Path | POST | /auth/register | 201 | ApiResponse wrapper + token saved |
| 2 | Register Duplicate | POST | /auth/register | 409 Conflict | Domain exception works |
| 3 | Register Missing Password | POST | /auth/register | 400 | @Valid validation |
| 4 | Login Happy Path | POST | /auth/login | 200 | New token + saved |
| 5 | Login Wrong Password | POST | /auth/login | 401 | Generic error message |
| 6 | Login Non-existent User | POST | /auth/login | 401 | No info leak |
| 7 | Assign Role Non-Admin | POST | /auth/admin/assign-role | 403 | Authorization works |
| 8 | Assign Role No Token | POST | /auth/admin/assign-role | 401/403 | Authentication required |

### User Service E2E (10 tests)

| # | Test | Method | Endpoint | Expected | Verify |
|---|---|---|---|---|---|
| 1 | Setup Register (via 8081) | POST | /auth/register | 201 | Get token cho user-service |
| 2 | Setup Login | POST | /auth/login | 200 | Refresh token |
| 3 | Get My Profile | GET | /users/me | 200 | ApiResponse, username matches |
| 4 | Get Profile No Token | GET | /users/me | 401 | Authentication enforced |
| 5 | Get Profile Invalid Token | GET | /users/me | 401 | Invalid JWT rejected |
| 6 | Update Profile Happy | PUT | /users/me | 200 | fullName + email updated |
| 7 | Update Invalid Email | PUT | /users/me | 400 | Validation works |
| 8 | List All Users (non-admin) | GET | /users | 403 | @PreAuthorize works |
| 9 | Delete User (non-admin) | DELETE | /users/{username} | 403 | Authorization works |
| 10 | Actuator Health | GET | /actuator/health | 200 | Public endpoint working |

---

## 🔍 Verify khi review với thầy

Sau khi chạy xong, screenshot/export ra để show:

1. **Collection Runner result:**
   - Tab "Run Results" sẽ hiển thị: **18 passed / 0 failed**
   - Mỗi test có thời gian response (ms)
   - Có cột "Iterations" để chạy nhiều lần verify stability

2. **Test details:**
   - Click vào test → xem từng `pm.test()` assertion PASS hay FAIL
   - Xem request/response body để verify đúng schema (ApiResponse wrapper)

3. **Export báo cáo:**
   - Postman có export sang JSON / HTML (qua Newman)
   - File HTML report đẹp, có color-coded PASS/FAIL

---

## 🧪 Trace logic test

### Auto-extract token
Test 1 (Register Happy) lưu token vào `{{token}}`. Các test sau dùng `Authorization: Bearer {{token}}`.

```js
// Test script trong "1. Register"
pm.environment.set('token', json.data.token);

// Trong các test sau, header:
// Authorization: Bearer {{token}}
```

### Random username mỗi run
Pre-request script generate random username → mỗi lần chạy không bị conflict.

```js
const randomSuffix = Math.floor(Math.random() * 1000000);
pm.environment.set('username', 'e2e_user_' + randomSuffix);
```

### Test ApiResponse wrapper consistency
Mọi test PASS đều check format mới:
```js
pm.expect(json.success).to.be.true;          // ApiResponse.success
pm.expect(json.data).to.be.an('object');     // data payload
pm.expect(json.message).to.be.a('string');   // message
pm.expect(json.timestamp).to.be.a('string'); // timestamp
```

---

## 🐛 Troubleshooting

### Test fail với "Could not connect"
→ Service chưa start. Check `docker ps` hoặc `curl localhost:8081/actuator/health`.

### Test 1 (Register) fail 500
→ Có thể DB chưa migrate. Check log auth-service xem Flyway có chạy không.

### Test 6 (Update Profile) fail 401
→ Token user-service không nhận. Có thể do user chưa sync qua Kafka từ auth-service → user-service. Đợi 5s sau setup steps rồi chạy lại, hoặc check Kafka consumer log của user-service.

### Test 8 (List Users) fail không phải 403
→ SecurityConfig của user-service chưa apply `@PreAuthorize`. Check `UserController.getAllUsers()`.

---

## 📊 Khi nào chạy E2E test này?

- ✅ Mỗi lần merge MR vào main
- ✅ Trước khi deploy production
- ✅ Sau khi update Spring Boot version
- ✅ Sau khi refactor security config
- ❌ Không cần chạy mỗi commit (đã có unit + integration test)

---

**Author:** Nguyễn Hiếu An | OJT R2S
**Date:** May 26, 2026
