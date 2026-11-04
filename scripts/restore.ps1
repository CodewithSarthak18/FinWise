# ==============================================================================
# FinWise Production Database Restore Script (PowerShell for Windows)
# ==============================================================================
param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$DbContainer = "finwise-mysql",
    [string]$DbName = "finwise_db",
    [string]$DbUser = "finwise_user",
    [string]$DbPassword = "finwise_pass_2026"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file does not exist: $BackupFile"
}

Write-Warning "You are about to restore database '$DbName' from '$BackupFile'."
Write-Warning "Current data will be overwritten!"
$confirmation = Read-Host "Type 'RESTORE' to continue"

if ($confirmation -ne "RESTORE") {
    Write-Host "[INFO] Operation cancelled." -ForegroundColor Yellow
    exit
}

Write-Host "[INFO] Restoring '$DbName'..." -ForegroundColor Cyan

Get-Content -Path $BackupFile -Raw | docker exec -i $DbContainer mysql `
    --user=$DbUser `
    --password=$DbPassword `
    $DbName

Write-Host "[SUCCESS] Database restoration complete!" -ForegroundColor Green
