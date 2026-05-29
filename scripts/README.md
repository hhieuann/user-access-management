# Scripts — Local development helpers

Bộ script PowerShell để chạy project nhanh hơn (không cần gõ env vars thủ công).

## 📁 Files

| Script | Mục đích |
|---|---|
| `start-infra.ps1` | Start Postgres + Kafka + Zookeeper tuần tự (tránh race condition) |
| `stop-infra.ps1` | Stop tất cả container (giữ data) |
| `stop-infra.ps1 -RemoveVolumes` | Stop + xóa volume (fresh start) |
| `run-auth-service.ps1` | Run auth-service local với profile=dev |
| `run-user-service.ps1` | Run user-service local với profile=dev |

## 🚀 Cách dùng

### Lần đầu setup

```powershell
# Terminal 1: Start infrastructure
.\scripts\start-infra.ps1

# Terminal 2: Start auth-service
.\scripts\run-auth-service.ps1

# Terminal 3: Start user-service
.\scripts\run-user-service.ps1
```

### Khi xong, dọn dẹp

```powershell
# Ctrl+C ở Terminal 2 & 3 để stop services

# Terminal 1: Stop infrastructure
.\scripts\stop-infra.ps1
```

### Khi Flyway báo checksum mismatch hoặc DB lỗi

```powershell
.\scripts\stop-infra.ps1 -RemoveVolumes
.\scripts\start-infra.ps1
# Sau đó start lại auth + user service
```

## ⚠️ Lưu ý

- Scripts dùng profile `dev` → connect Postgres ở `localhost:5433` (match docker-compose)
- JWT_SECRET dùng fallback dev (đủ cho local, KHÔNG dùng production)
- Production deploy dùng profile `docker` hoặc `prod` qua docker compose / Kubernetes

## 🔓 Nếu PowerShell chặn script

Lần đầu chạy có thể gặp:
```
... cannot be loaded because running scripts is disabled on this system.
```

Fix: chạy 1 lần lệnh sau (Run as Administrator):
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```
Sau đó scripts chạy bình thường.
