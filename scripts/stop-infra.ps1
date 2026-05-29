# =========================================================
# Stop infrastructure containers (giu data trong volume)
# =========================================================
# Cach dung:
#   .\scripts\stop-infra.ps1
# Neu muon XOA DATA hoan toan (fresh start), them flag -RemoveVolumes:
#   .\scripts\stop-infra.ps1 -RemoveVolumes
# =========================================================

param(
    [switch]$RemoveVolumes
)

if ($RemoveVolumes) {
    Write-Host "[WARN] Removing all volumes - DATA WILL BE LOST!" -ForegroundColor Red
    docker compose down -v
} else {
    Write-Host "[INFO] Stopping containers (preserving data)..." -ForegroundColor Cyan
    docker compose down
}

Write-Host "[OK] Infrastructure stopped." -ForegroundColor Green
