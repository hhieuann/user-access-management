# 🔧 CI/CD End-to-End Verification - Troubleshooting Journey

> Tài liệu tổng hợp quá trình chứng minh CI/CD pipeline thực sự deploy thành công.
>
> **Sinh viên:** Nguyễn Hiếu An | FPT HCMC | OJT R2S
> **Dự án:** User Access Management System (UAM)
> **Date:** May 2026

---

## 📑 Mục lục

1. [Bối cảnh & Yêu cầu](#1-bối-cảnh--yêu-cầu)
2. [Tổng quan 5 vấn đề gặp phải](#2-tổng-quan-5-vấn-đề-gặp-phải)
3. [Vấn đề 1: Pipeline pass nhưng image cũ vẫn deploy](#3-vấn-đề-1-pipeline-pass-nhưng-image-cũ-vẫn-deploy)
4. [Vấn đề 2: Docker layer cache không invalidate](#4-vấn-đề-2-docker-layer-cache-không-invalidate)
5. [Vấn đề 3: Database schema cũ không tương thích code mới](#5-vấn-đề-3-database-schema-cũ-không-tương-thích-code-mới)
6. [Vấn đề 4: Volume Docker chứa data DB cũ](#6-vấn-đề-4-volume-docker-chứa-data-db-cũ)
7. [Vấn đề 5: Race condition Kafka - Zookeeper](#7-vấn-đề-5-race-condition-kafka---zookeeper)
8. [Bài học lớn rút ra](#8-bài-học-lớn-rút-ra)
9. [Quy trình verification chuẩn](#9-quy-trình-verification-chuẩn)
10. [Bằng chứng cuối cho thầy](#10-bằng-chứng-cuối-cho-thầy)

---

## 1. Bối cảnh & Yêu cầu

### Yêu cầu của thầy

Sau khi CI/CD pipeline có 3 stages ✅ xanh (build, test, deploy), thầy yêu cầu **chứng minh thực sự** rằng deploy đã thành công bằng cách:

- Pull image từ Docker Hub về local
- Run image đó
- Gọi API → verify response đúng

> **Lý do:** Icon ✅ chỉ nghĩa "lệnh chạy không lỗi", **không** nghĩa "image deploy đúng code mới và hoạt động đúng". Pipeline có thể pass nhưng output thực sự lại sai.

### Đây là **best practice industry**

Trong công ty thật:
- DevOps không tin pipeline xanh là xong
- Phải có **smoke test** sau deploy
- Tốt nhất: tự động hóa luôn (canary deployment, blue-green)

---

## 2. Tổng quan 5 vấn đề gặp phải

| # | Vấn đề | Triệu chứng | Root cause |
|---|--------|-------------|------------|
| 1 | Pipeline pass nhưng image cũ | Postman fail dù đã merge code mới | `.gitlab-ci.yml` skip tests + dùng cache |
| 2 | Docker layer cache | Build xong digest vẫn giống image cũ | Thiếu `--no-cache` |
| 3 | Schema mismatch | API trả 500: `column role does not exist` | File migration V1 cũ, code đã refactor |
| 4 | Volume Docker persistent | DB vẫn schema cũ dù đã push V2 | Volume `postgres_data` cũ |
| 5 | Race condition Kafka | Kafka exit code 1 sau `down -v` | Zookeeper chưa ready khi Kafka start |

---

## 3. Vấn đề 1: Pipeline pass nhưng image cũ vẫn deploy

### Triệu chứng

- ✅ 3 stages xanh trên GitLab CI/CD
- ✅ Image trên Docker Hub có timestamp mới
- ❌ Pull image về test → digest **giống hệt** image cũ (`sha256:2adc3fccd18c...`)
- ❌ Postman test fail giống như chưa fix gì

### Phân tích `.gitlab-ci.yml` ban đầu

```yaml
deploy-job:
  stage: deploy
  only:
    - main
  tags:
    - local
  script:
    - mvn clean package -DskipTests   # ❌ Skip tests
    - docker login -u $env:DOCKERHUB_USERNAME -p $env:DOCKERHUB_TOKEN
    - docker build -t "$($env:DOCKERHUB_USERNAME)/auth-service:latest" ./auth-service   # ❌ Có cache
    - docker push "$($env:DOCKERHUB_USERNAME)/auth-service:latest"
```

### Vấn đề

1. `-DskipTests`: deploy không chạy test → không phát hiện regression
2. `docker build` không có `--no-cache`: dùng layer cache cũ
3. Pipeline báo "thành công" vì lệnh không lỗi → người dùng tin nhầm

### Fix

```yaml
deploy-job:
  script:
    - echo "Verifying current commit..."
    - git log -1 --pretty=format:"%H %s"
    - echo "Building JAR files (with tests)..."
    - mvn clean install   # ✅ Chạy tests trước khi build
    - echo "Logging in to Docker Hub..."
    - docker login -u $env:DOCKERHUB_USERNAME -p $env:DOCKERHUB_TOKEN
    - echo "Building auth-service image (no cache)..."
    - docker build --no-cache --pull -t "$($env:DOCKERHUB_USERNAME)/auth-service:latest" ./auth-service   # ✅ Force rebuild
    - docker build --no-cache --pull -t "$($env:DOCKERHUB_USERNAME)/user-service:latest" ./user-service
    - docker push "$($env:DOCKERHUB_USERNAME)/auth-service:latest"
    - docker push "$($env:DOCKERHUB_USERNAME)/user-service:latest"
```

### Bằng chứng đã fix

So sánh digest trước/sau:

| Service | Digest cũ | Digest mới (sau fix) |
|---------|-----------|----------------------|
| auth-service | `sha256:2adc3fccd18c...` | `sha256:c4436c8e58fc...` ✅ |
| user-service | `sha256:dd44109b83ff...` | `sha256:a74a675622e6...` ✅ |

### Bài học

> **Pipeline xanh ≠ Deploy đúng.** Cần verify bằng smoke test + so sánh digest image.

---

## 4. Vấn đề 2: Docker layer cache không invalidate

### Nguyên nhân kỹ thuật

Docker build có cơ chế **layer caching** để tăng tốc:

```dockerfile
FROM eclipse-temurin:17-jre-jammy
COPY target/app.jar app.jar   # ← Layer này được cache
RUN ...
```

Nếu Docker nghĩ file `.jar` không đổi (theo size + timestamp) → dùng cache cũ → image y hệt bản trước.

### Trong GitLab Runner local

GitLab Runner chạy trên Windows local của bạn. Mỗi lần build, Docker daemon **giữ lại layer cache** trong filesystem.

→ Code mới được build vào `.jar`, nhưng Docker không phát hiện sự khác biệt ở layer COPY → reuse cache → image cũ.

### Fix

Thêm 2 flag vào `docker build`:

```bash
docker build --no-cache --pull -t image:latest .
```

- `--no-cache`: bỏ qua layer cache, rebuild toàn bộ
- `--pull`: pull base image mới từ Docker Hub (đề phòng base image update)

### Trade-off

| | Có cache | `--no-cache --pull` |
|---|----------|---------------------|
| Tốc độ build | Nhanh (~30s) | Chậm (2-5 phút) |
| Độ chính xác | Có thể outdated | Luôn fresh |
| Dùng khi | Dev iteration nhanh | CI/CD production |

### Bài học

> **Cache là dao 2 lưỡi.** Dev local dùng cache OK, nhưng CI/CD production deploy phải `--no-cache` để guarantee fresh build.

---

## 5. Vấn đề 3: Database schema cũ không tương thích code mới

### Triệu chứng

Sau khi image mới đã deploy đúng (digest mới), app start OK nhưng:

```
ERROR: column u1_0.role does not exist
SQL Error: 0, SQLState: 42703
```

→ API trả 500 ở mọi request liên quan đến User.

### Root cause: Schema drift

File `V1__init_auth_schema.sql` (Flyway) được viết theo schema **CŨ**:

```sql
-- V1: Schema CŨ (UUID + many-to-many)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
```

Nhưng code Java entity đã refactor thành schema **MỚI**:

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue
    private Long id;          // Long, không phải UUID
    
    private String username;
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;        // Column "role" trực tiếp trong table users
}
```

→ Code đọc column `role`, DB không có → fail.

### Tại sao không phát hiện sớm?

| Môi trường | Vì sao OK? |
|------------|-----------|
| Mvn local | DB local có thể đã được sửa tay hoặc auto-migrate khác |
| H2 in-memory test | Hibernate `ddl-auto=create-drop` tự tạo schema theo entity |
| Docker production | Postgres trống → Flyway chạy V1 → schema cũ → fail |

### Fix: Tạo V2 migration

File mới: `auth-service/src/main/resources/db/migration/V2__refactor_users_table.sql`

```sql
-- V2: Refactor users table to match new entity (Long id, role column)
-- Drop bảng cũ (many-to-many không còn dùng)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;

-- Drop bảng users cũ và tạo lại với schema mới
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER'
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
```

### Bài học

> **Flyway migrations là một phần của codebase.** Mỗi lần thay đổi entity → tạo migration mới. Không bao giờ sửa migration đã merge (vì checksum sẽ mismatch ở môi trường khác).

> **Đừng dùng `ddl-auto=update`** trong production. Mất control, không trace được, có thể mất data.

---

## 6. Vấn đề 4: Volume Docker chứa data DB cũ

### Triệu chứng

Sau khi push V2 migration, build image mới, nhưng:
- App vẫn fail với cùng error `column role does not exist`
- Verify DB: schema vẫn cũ (không có column `role`)

### Root cause: Volume persistence

Docker Compose lưu data DB trong **named volume**:

```yaml
volumes:
  postgres_data:

services:
  postgres-db:
    volumes:
      - postgres_data:/var/lib/postgresql
```

→ Khi `docker compose down`, container bị xóa nhưng **volume vẫn còn**.

Khi `docker compose up -d` lại:
- Container postgres mới được tạo
- Nhưng dùng lại volume cũ → DB đã có data + schema cũ
- `init.sql` **không re-run** (vì DB đã tồn tại)
- Flyway thấy `flyway_schema_history` đã có V1 → skip
- Schema cũ vẫn nguyên → app fail

### Fix

Dùng flag `-v` để xóa cả volume:

```powershell
docker compose down -v   # ⚠️ Xóa toàn bộ data
```

→ Khi up lại, postgres tạo DB từ đầu, init.sql + Flyway V1 + V2 chạy đầy đủ.

### Verify

```powershell
# List volumes của project
docker volume ls | findstr user-access

# Trước khi down -v: thấy volume
# user-access-management_postgres_data

# Sau khi down -v: không còn
```

### Bài học

> **Docker volumes persist data**, kể cả khi container bị xóa. Để test fresh deployment, phải `docker compose down -v`.

> **Đây cũng là lý do** production phải có backup database trước mỗi deployment. Vì nếu lỡ xóa volume, mất hết data.

---

## 7. Vấn đề 5: Race condition Kafka - Zookeeper

### Triệu chứng

Sau khi `docker compose down -v` và up lại:

```
✘ Container kafka-1   Error dependency kafka failed to start
dependency failed to start: container kafka-1 exited (1)
```

Log Kafka:
```
[ERROR] Timed out waiting for connection to Zookeeper server [zookeeper:2181].
[WARN] Client session timed out, have not heard from server in 40000ms
```

### Root cause: Race condition

`docker compose up -d` start tất cả services **song song**:

```
T=0s: zookeeper container created, đang init
T=0s: kafka container created, đang init
T=2s: kafka cố connect zookeeper:2181 → zookeeper chưa accept connection
T=40s: kafka timeout → exit code 1
T=45s: zookeeper mới sẵn sàng (quá muộn)
```

### Healthcheck không đủ

```yaml
kafka:
  healthcheck:
    test: ["CMD-SHELL", "nc -z localhost 9092 || exit 1"]
    start_period: 30s   # ← Không đủ
```

`start_period: 30s` chỉ delay healthcheck, không delay Kafka start.

### Fix tạm thời: Start tuần tự

```powershell
docker compose up -d zookeeper
Start-Sleep -Seconds 15

docker compose up -d kafka
Start-Sleep -Seconds 30   # Kafka cần thời gian dài hơn

docker compose up -d
```

### Fix lâu dài (đề xuất)

Sửa `docker-compose.yaml`:

```yaml
kafka:
  depends_on:
    zookeeper:
      condition: service_healthy   # Đợi zookeeper healthy trước
  healthcheck:
    start_period: 60s   # Tăng từ 30s lên 60s
```

Hoặc dùng wait-script trong Kafka container:
```yaml
kafka:
  command: >
    bash -c "
      while ! nc -z zookeeper 2181; do echo 'Waiting for zookeeper...'; sleep 2; done;
      /etc/confluent/docker/run
    "
```

### Bài học

> **Distributed systems có race conditions.** Service A phụ thuộc service B → cần explicit wait (healthcheck, retry, polling), không chỉ dựa vào `depends_on`.

> **Production solution:** Kubernetes có `readinessProbe` để giải quyết tương tự, mature hơn Docker Compose.

---

## 8. Bài học lớn rút ra

### 8.1. Trust but Verify

> Pipeline ✅ ≠ Deploy thực sự thành công.

Cần verify bằng:
- Pull image về test
- Smoke test API
- So sánh image digest trước/sau
- Monitoring sau deploy

### 8.2. Cache là dao 2 lưỡi

| Khi nào dùng cache | Khi nào không dùng |
|--------------------|---------------------|
| Dev local iteration nhanh | CI/CD production deploy |
| Build dependencies không đổi | Build artifact (app code) |
| Testing | Release tag/version |

### 8.3. Database migrations là code

- Mỗi entity thay đổi → migration mới
- Migration đã merge **không bao giờ sửa**
- `ddl-auto=update` ❌ production
- Backup trước mỗi major migration

### 8.4. Volumes persist data

- Container chết, volume còn
- Test fresh deploy: `down -v`
- Production deploy: **không bao giờ** `down -v`, vì mất data

### 8.5. Distributed systems cần khởi động đúng thứ tự

- `depends_on` chỉ check container started, không check service ready
- Cần `condition: service_healthy` + healthcheck đúng
- Hoặc retry logic ở consumer side

### 8.6. End-to-end verification là chuẩn industry

Thầy yêu cầu chứng minh bằng API là **best practice**. Trong công ty thật:
- Canary deployment: deploy 1% traffic, monitor metrics, rollout dần
- Blue-green deployment: 2 environments song song, switch khi verified
- Automated smoke test trong pipeline

---

## 9. Quy trình verification chuẩn

Sau bài học này, đây là quy trình mình đề xuất khi muốn verify CI/CD deploy:

### Bước 1: Pre-deployment

```powershell
# Verify code đã push
git log -1 --pretty=format:"%H %s"

# Verify branch hiện tại là main
git branch --show-current
```

### Bước 2: Trigger pipeline

```powershell
git push origin main
```

Hoặc merge MR vào main.

### Bước 3: Verify pipeline

1. Vào **GitLab → CI/CD → Pipelines**
2. Đợi pipeline mới nhất chạy xong
3. Verify 3 stages ✅
4. Click vào **deploy-job** → check log:
   - `git log -1` hiện đúng commit
   - `mvn clean install` BUILD SUCCESS với tests pass
   - `docker build --no-cache --pull` không có "CACHED"
   - `docker push` thành công với digest mới

### Bước 4: Verify Docker Hub

1. Mở https://hub.docker.com/u/{username}
2. Vào repo `auth-service`, tab Tags
3. Tag `latest`:
   - Timestamp = vài phút trước (sau pipeline)
   - Digest khác lần trước

### Bước 5: Local smoke test

```powershell
# Stop containers cũ + xóa volumes
docker compose down -v

# Xóa image local để force pull
docker rmi {username}/auth-service:latest
docker rmi {username}/user-service:latest

# Pull image mới
docker pull {username}/auth-service:latest
docker pull {username}/user-service:latest

# Verify digest mới
docker images | findstr {username}

# Start tuần tự
docker compose up -d zookeeper
Start-Sleep -Seconds 15
docker compose up -d kafka
Start-Sleep -Seconds 30
docker compose up -d

# Verify 6 containers running
docker ps
```

### Bước 6: API verification

#### Health check
```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
```

Expected: `status: UP`

#### Postman E2E test
1. Mở Postman → Run collection `Auth E2E All-in-One`
2. Expected: 6/6 PASS
   - Register: 200
   - Login: 200
   - Profile: 200
   - Wrong password: 401
   - No token: 403
   - Duplicate username: 400

### Bước 7: Document evidence

Chụp screenshot:
1. GitLab pipeline 3 stages ✅
2. Docker Hub tags với timestamp
3. `docker images` show digest mới
4. `docker ps` show containers từ image Hub
5. Health check response `{"status":"UP"}`
6. Postman Collection Runner 6/6 PASS

→ Tất cả file vào folder `evidence/` hoặc gắn vào báo cáo Word.

---

## 10. Bằng chứng cuối cho thầy

### Checklist verification

| # | Bằng chứng | File / Location | Status |
|---|------------|-----------------|--------|
| 1 | Pipeline 3 stages ✅ | Screenshot GitLab CI/CD | ✅ |
| 2 | Image digest mới trên Hub | Screenshot hub.docker.com | ✅ |
| 3 | `docker pull` newer image | Terminal output | ✅ |
| 4 | `docker images` digest mới | Terminal output | ✅ |
| 5 | `docker ps` containers từ image Hub | Terminal output | ✅ |
| 6 | App started successfully | `docker logs` | ✅ |
| 7 | Health check 200 OK | curl / Invoke-RestMethod | ✅ |
| 8 | Postman 6/6 PASS | Collection Runner screenshot | ✅ |

### Statement cho thầy

> **Em đã chứng minh CI/CD pipeline thực sự deploy thành công thông qua chuỗi verification end-to-end:**
>
> 1. ✅ Code được push lên main branch
> 2. ✅ Pipeline trên GitLab tự động trigger, chạy 3 stages (build → test → deploy) thành công
> 3. ✅ Image mới được build với `--no-cache` và push lên Docker Hub
> 4. ✅ Em pull image về local, verify digest mới khác bản cũ
> 5. ✅ Run containers từ image Docker Hub
> 6. ✅ Health check API trả `status: UP`
> 7. ✅ Postman E2E test 6/6 PASS (Register, Login, Profile, 3 Negative cases)
>
> **Kết luận:** Code đã được automated test, build, deploy lên production registry, và verified hoạt động đúng qua API thật. Pipeline CI/CD đáp ứng chuẩn industry.

---

## 📚 Tài liệu tham khảo

- [Docker BuildKit Caching](https://docs.docker.com/build/cache/)
- [Flyway Best Practices](https://documentation.red-gate.com/fd/best-practices-184127572.html)
- [Docker Compose depends_on](https://docs.docker.com/compose/compose-file/05-services/#depends_on)
- [GitLab CI/CD docs](https://docs.gitlab.com/ee/ci/)
- [Kubernetes Probes](https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/)

---

**Author:** Nguyễn Hiếu An  
**Project:** User Access Management System (UAM) - OJT R2S  
**Date:** May 11, 2026

> 💡 *Bài học quan trọng nhất: Pipeline ✅ ≠ Deploy thành công. Always verify end-to-end với smoke test API.*
