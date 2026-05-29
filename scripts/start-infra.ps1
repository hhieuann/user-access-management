# =========================================================
# Start infrastructure (Postgres + Kafka + Zookeeper) tuan tu
# Tranh race condition Kafka <-> Zookeeper
# =========================================================
# Cach dung:
#   .\scripts\start-infra.ps1
# =========================================================

$ErrorActionPreference = "Stop"

Write-Host "[1/3] Starting zookeeper..." -ForegroundColor Cyan
docker compose up -d zookeeper
Start-Sleep -Seconds 15

Write-Host "[2/3] Starting kafka..." -ForegroundColor Cyan
docker compose up -d kafka
Start-Sleep -Seconds 30

Write-Host "[3/3] Starting postgres..." -ForegroundColor Cyan
docker compose up -d postgres-db
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "[OK] Infrastructure started. Container status:" -ForegroundColor Green
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  Terminal 2: .\scripts\run-auth-service.ps1"
Write-Host "  Terminal 3: .\scripts\run-user-service.ps1"
