# =========================================================
# Run auth-service locally with dev profile
# =========================================================
# Cach dung:
#   .\scripts\run-auth-service.ps1
# Yeu cau:
#   - Postgres + Kafka da chay (docker compose up -d postgres-db kafka zookeeper)
# =========================================================

$ErrorActionPreference = "Stop"

Write-Host "[INFO] Setting Spring profile = dev" -ForegroundColor Cyan
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "[INFO] Starting auth-service on port 8081..." -ForegroundColor Cyan
Write-Host "[INFO] Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

mvn spring-boot:run -pl auth-service
