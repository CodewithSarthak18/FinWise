# ==============================================================================
# FinWise Production Database Backup Script (PowerShell for Windows)
# ==============================================================================
param(
    [string]$BackupDir = "./backups",
    [string]$DbContainer = "finwise-mysql",
    [string]$DbName = "finwise_db",
    [string]$DbUser = "finwise_user",
    [string]$DbPassword = "finwise_pass_2026",
    [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $BackupDir "finwise_backup_$timestamp.sql"
$zipFile = "$backupFile.gz"

Write-Host "[INFO] Initiating database backup for '$DbName'..." -ForegroundColor Cyan

# Run mysqldump inside container
docker exec $DbContainer mysqldump `
    --user=$DbUser `
    --password=$DbPassword `
    --single-transaction `
    --quick `
    --routines `
    --triggers `
    --databases $DbName > $backupFile

# Compute SHA256 Hash
$hash = (Get-FileHash -Path $backupFile -Algorithm SHA256).Hash
Set-Content -Path "$backupFile.sha256" -Value "$hash  $backupFile"

Write-Host "[SUCCESS] Backup generated at: $backupFile" -ForegroundColor Green
Write-Host "[INFO] SHA256: $hash" -ForegroundColor Yellow

# Prune older backups
$cutoffDate = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -Path $BackupDir -Filter "finwise_backup_*.sql*" | Where-Object { $_.LastWriteTime -lt $cutoffDate } | Remove-Item -Force
Write-Host "[INFO] Purged backups older than $RetentionDays days." -ForegroundColor Gray
