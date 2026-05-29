# GitLab CI/CD Code Review: .gitlab-ci.yml (Round 6)
**Ngày**: 2026-05-25  
**Reviewer**: Kỳ Lê

Dưới đây là phần review chi tiết các điểm cần khắc phục trong file `.gitlab-ci.yml` hiện tại và lý do "Tại sao" theo chuẩn Production-Ready:

### 1. Lỗi Nghiêm Trọng (Critical): Sai lầm giữa `Artifacts` và `Cache`
* **Vấn đề:** Trong `build-job`, em đang khai báo `.m2/repository` vào trong `artifacts`.
* **Tại sao sai?** 
  * `Cache` dùng để lưu trữ các thư viện tải từ Internet, giúp các lần chạy sau nhanh hơn. Nó được nén và lưu trên Runner.
  * `Artifacts` dùng để **chuyền kết quả** từ Job này sang Job khác (hoặc cho user tải về). File này sẽ được upload trực tiếp lên máy chủ GitLab.
  * Thư mục `.m2` có thể nặng hàng GB. Việc ép GitLab upload/download hàng GB `.m2` ở mỗi pipeline sẽ làm sập băng thông mạng, phình to dung lượng ổ cứng của GitLab Server và khiến pipeline của em chạy chậm rì.
* **Cách sửa:** Bỏ `.m2/repository` ra khỏi `artifacts`. Artifacts chỉ nên chứa kết quả cuối cùng: `target/*.jar`.

* **Ưu tiên: P0**

### 2. Tư duy "Máy Tôi Chạy Được" (Local vs. Production)
* **Vấn đề:** Em đang fix cứng `tags: [local, mac]` và biến `TESTCONTAINERS_HOST_OVERRIDE: "host.docker.internal"`.
* **Tại sao sai?** Môi trường CI/CD chuẩn là các Server Linux chạy các Runner độc lập (Stateless). Việc "trói" pipeline vào máy Mac cá nhân của em đồng nghĩa với việc: Nếu em tắt máy hoặc đi nghỉ phép, cả team sẽ bị tê liệt CI/CD. Ngoài ra, `host.docker.internal` là đặc quyền của Docker Desktop (Mac/Windows), lên server Linux nó sẽ lỗi không tìm thấy host.
* **Cách sửa:** Gỡ bỏ các tag cá nhân, chuyển sang dùng Shared Runner của team/công ty (thường là môi trường Linux). Bỏ luôn host override của Testcontainers.
* **Ưu tiên: P3**

### 3. "Deploy" Stage chưa thực sự là Deploy
* **Vấn đề:** Ở `deploy-job`, em dùng `docker compose up -d --build` ngay trên máy chạy CI.
* **Tại sao sai?** 
  * GitLab Runner sinh ra để **thực thi tác vụ**, không phải là Server để **chạy ứng dụng** (Hosting). Nếu làm thế này, app của em đang được host ngay trên máy CI.
  * Trong một quy trình chuẩn, ta sẽ có stage `docker-build`: Build ra các Docker Image, đánh tag theo phiên bản (`$CI_COMMIT_SHA`) và **Push** lên Container Registry (Docker Hub, AWS ECR, GitLab Registry).
  * Stage `deploy` thực sự sẽ dùng SSH hoặc Agent kết nối vào con Server Production để ra lệnh: "Hãy pull image mới nhất trên Registry về và chạy đi".
* **Ưu tiên: P3**

### 4. Bỏ lỡ tính năng Report cực mạnh của GitLab
* **Vấn đề:** Em chạy `mvn test` và `verify`, nhưng nếu có test case rớt, team sẽ phải căng mắt đọc hàng ngàn dòng log console.
* **Cách sửa:** GitLab hỗ trợ đọc file XML do Maven sinh ra và hiển thị trực tiếp lên giao diện Merge Request. Em cần bổ sung `artifacts: reports: junit` để bắt các file `target/surefire-reports/TEST-*.xml`. 
* **Ưu tiên: P0**

### 5. Các tối ưu nhỏ khác (Clean Code & Perf)
* Không nên chạy `chmod +x mvnw` trong CI. Hãy cấp quyền ngay dưới máy em và commit lên Git: `git update-index --chmod=+x mvnw`.
* Biến `POSTGRES_PASSWORD` không nên hardcode. Dù là test, hãy tập thói quen khai báo nó ở dạng biến ẩn (Masked Variable) trong Settings của GitLab để không lộ Secret.
* **Ưu tiên: P0**
---

### Mẫu CI/CD Chuẩn (Template) tham khảo

```yaml
image: maven:3.9.6-eclipse-temurin-17

# Khai báo cache global cho Maven
cache:
  key:
    files:
      - pom.xml
  paths:
    - .m2/repository

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

stages:
  - build
  - test
  - docker-build
  - deploy

build-job:
  stage: build
  script:
    - ./mvnw clean package -DskipTests
  artifacts:
    paths:
      - "*/target/*.jar"
    expire_in: 1 day # Chỉ giữ 1 ngày để tiết kiệm dung lượng GitLab

test-job:
  stage: test
  script:
    - ./mvnw test
    - ./mvnw verify -pl e2e-tests
  artifacts:
    when: always
    reports:
      junit:
        - "*/target/surefire-reports/TEST-*.xml"
        - "*/target/failsafe-reports/TEST-*.xml"

# Ở thực tế, job này sẽ dùng Kaniko hoặc Docker-in-Docker để build image và push lên Registry
docker-build-job:
  stage: docker-build
  image: docker:25.0.3
  services:
    - docker:25.0.3-dind
  script:
    - docker build -t my-registry/auth-service:$CI_COMMIT_SHA ./auth-service
    - docker build -t my-registry/user-service:$CI_COMMIT_SHA ./user-service
    # - docker push my-registry/auth-service:$CI_COMMIT_SHA

# Dùng SSH để trigger server production pull image mới về
deploy-prod:
  stage: deploy
  image: alpine:latest
  only:
    - main # Chỉ deploy khi merge code vào nhánh main
  script:
    - echo "Deploying to Production Server..."
    # Lệnh giả lập: ssh user@prod-server "docker compose pull && docker compose up -d"

```

### Kết luận: 

Em mới làm quen với CI/CD thường dùng chính máy tính cá nhân cài GitLab Runner để làm server chạy thử nghiệm nên ưu tiên xử lý P0


