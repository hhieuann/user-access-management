# =========================================================
# Run user-service locally with dev profile
# =========================================================
# Cach dung:
#   .\scripts\run-user-service.ps1
# =========================================================

$ErrorActionPreference = "Stop"

Write-Host "[INFO] Setting Spring profile = dev" -ForegroundColor Cyan
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "[INFO] Starting user-service on port 8082..." -ForegroundColor Cyan
Write-Host "[INFO] Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

mvn spring-boot:run -pl user-service
