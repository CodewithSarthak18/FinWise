#!/usr/bin/env bash
# ==============================================================================
# FinWise Production Database Restore Script
# Restores a compressed SQL dump into the running MySQL container
# ==============================================================================
set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path_to_backup_file.sql.gz>"
    exit 1
fi

BACKUP_FILE="$1"
DB_CONTAINER="${DB_CONTAINER:-finwise-mysql}"
DB_NAME="${DB_NAME:-finwise_db}"
DB_USER="${DB_USERNAME:-finwise_user}"
DB_PASS="${DB_PASSWORD:-finwise_pass_2026}"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "[ERROR] Backup file not found: ${BACKUP_FILE}"
    exit 1
fi

# Verify checksum if .sha256 file exists
CHECKSUM_FILE="${BACKUP_FILE}.sha256"
if [ -f "${CHECKSUM_FILE}" ]; then
    echo "[INFO] Verifying backup checksum..."
    sha256sum -c "${CHECKSUM_FILE}"
fi

echo "[WARNING] You are about to restore database '${DB_NAME}' from ${BACKUP_FILE}."
echo "[WARNING] Current database data will be overwritten!"
read -p "Type 'RESTORE' to continue: " CONFIRMATION

if [ "${CONFIRMATION}" != "RESTORE" ]; then
    echo "[INFO] Restore aborted by user."
    exit 0
fi

echo "[INFO] Restoring database '${DB_NAME}' from ${BACKUP_FILE}..."

if [[ "${BACKUP_FILE}" == *.gz ]]; then
    gunzip -c "${BACKUP_FILE}" | docker exec -i "${DB_CONTAINER}" mysql \
        --user="${DB_USER}" \
        --password="${DB_PASS}" \
        "${DB_NAME}"
else
    cat "${BACKUP_FILE}" | docker exec -i "${DB_CONTAINER}" mysql \
        --user="${DB_USER}" \
        --password="${DB_PASS}" \
        "${DB_NAME}"
fi

echo "[SUCCESS] Database restoration completed successfully!"
