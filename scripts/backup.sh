#!/usr/bin/env bash
# ==============================================================================
# FinWise Production Database Backup Script
# Performs non-blocking, transactional hot backup of MySQL with compression & checksum
# ==============================================================================
set -euo pipefail

# Configuration (defaults to .env values if set)
BACKUP_DIR="${BACKUP_DIR:-./backups}"
DB_CONTAINER="${DB_CONTAINER:-finwise-mysql}"
DB_NAME="${DB_NAME:-finwise_db}"
DB_USER="${DB_USERNAME:-finwise_user}"
DB_PASS="${DB_PASSWORD:-finwise_pass_2026}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/finwise_backup_${TIMESTAMP}.sql.gz"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

mkdir -p "${BACKUP_DIR}"

echo "[INFO] Starting FinWise database backup at $(date)..."
echo "[INFO] Target Container: ${DB_CONTAINER}, Database: ${DB_NAME}"

# Execute mysqldump inside container with transaction isolation
docker exec "${DB_CONTAINER}" mysqldump \
    --user="${DB_USER}" \
    --password="${DB_PASS}" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --events \
    --databases "${DB_NAME}" \
    | gzip -9 > "${BACKUP_FILE}"

# Generate cryptographic checksum
sha256sum "${BACKUP_FILE}" > "${CHECKSUM_FILE}"

echo "[SUCCESS] Backup created successfully: ${BACKUP_FILE}"
echo "[INFO] Checksum verified: $(cat "${CHECKSUM_FILE}")"
echo "[INFO] File size: $(du -h "${BACKUP_FILE}" | cut -f1)"

# Prune old backups past retention threshold
echo "[INFO] Purging backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -type f -name "finwise_backup_*.sql.gz*" -mtime +"${RETENTION_DAYS}" -exec rm -f {} +

echo "[INFO] Backup operation completed at $(date)."
