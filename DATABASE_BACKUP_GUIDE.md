# FinWise Database Backup & Disaster Recovery Guide

---

## 1. Overview & Strategy

The FinWise persistence layer relies on **MySQL 8.0 InnoDB** with strict ACID transaction guarantees. This guide establishes our disaster recovery (DR) strategy, automated backup operations, retention policy, and emergency recovery drills.

### Recovery Objectives
- **Recovery Point Objective (RPO)**: $< 15$ minutes (leveraging daily transactional hot dumps + binary logs).
- **Recovery Time Objective (RTO)**: $< 30$ minutes (automated script-driven restoration).

---

## 2. Backup Mechanisms

### A. Non-Blocking Transactional Hot Backups (`mysqldump`)
Hot backups are executed using MySQL's `--single-transaction` mode, which creates a consistent snapshot across all InnoDB tables without acquiring table read locks or interrupting user transactions.

#### What is Included in Each Dump:
- Complete database schema & tables (`users`, `accounts`, `transactions`, `transfers`, `budgets`, etc.)
- Stored routines, triggers, and events (`--routines --triggers --events`)
- Quick streaming without buffering large tables into memory (`--quick`)

### B. Point-in-Time Recovery (PITR) via Binary Logs (`binlog`)
MySQL binary logging is configured in `docker/mysql/my.cnf`:
```ini
log_bin = /var/log/mysql/mysql-bin.log
binlog_format = ROW
binlog_expire_logs_seconds = 604800 # 7-day retention
sync_binlog = 1
```
In the event of an unintended deletion or hardware failure at 14:22, the database can be restored to the 02:00 snapshot, and transactions up to 14:21:59 can be replayed from the binary log.

---

## 3. Automated Cron Job Configuration

To execute automated daily backups at **02:30 AM UTC** and clean up backups older than 30 days:

### Linux / Production Host Crontab
Edit the host crontab (`crontab -e`):
```bash
# FinWise Daily Database Hot Backup at 02:30 AM
30 2 * * * cd /opt/finwise && ./scripts/backup.sh >> /var/log/finwise/backup-cron.log 2>&1
```

### Off-Site Replication to Cloud Storage (AWS S3 Example)
Append automated offsite sync to your backup pipeline:
```bash
# Sync compressed backup directory to private S3 bucket with AES-256 encryption
aws s3 sync /opt/finwise/backups s3://finwise-db-backups-secure/daily/ \
    --sse aws:kms \
    --exclude "*" \
    --include "*.sql.gz*"
```

---

## 4. Manual Execution Instructions

### Creating a Backup
- **Linux / macOS**:
  ```bash
  ./scripts/backup.sh
  ```
- **Windows (PowerShell)**:
  ```powershell
  .\scripts\backup.ps1
  ```

Output files will be generated in `./backups`:
```
backups/
├── finwise_backup_20261103_023000.sql.gz
└── finwise_backup_20261103_023000.sql.gz.sha256
```

### Restoring a Backup
1. **Linux / macOS**:
   ```bash
   ./scripts/restore.sh ./backups/finwise_backup_20261103_023000.sql.gz
   ```
2. **Windows (PowerShell)**:
   ```powershell
   .\scripts\restore.ps1 -BackupFile .\backups\finwise_backup_20261103_023000.sql
   ```

---

## 5. Point-in-Time Recovery (PITR) Walkthrough

If data corruption occurs at `2026-11-03 14:15:00`:

1. **Restore the most recent daily snapshot**:
   ```bash
   ./scripts/restore.sh ./backups/finwise_backup_20261103_023000.sql.gz
   ```
2. **Extract and apply incremental transactions up to the exact target second**:
   ```bash
   docker exec -it finwise-mysql mysqlbinlog \
       --start-datetime="2026-11-03 02:30:00" \
       --stop-datetime="2026-11-03 14:14:59" \
       /var/log/mysql/mysql-bin.000001 \
       | docker exec -i finwise-mysql mysql -u finwise_user -p finwise_db
   ```
3. **Verify data integrity**:
   Query transaction counts and latest account balances against the audit ledger.

---

## 6. Disaster Recovery Drill Checklist

Quarterly verification must validate the following steps in an isolated staging environment:
- [ ] Fresh container initialization using `docker compose up -d`.
- [ ] Execution of `./scripts/restore.sh` using a production backup sample.
- [ ] Verification of SHA-256 checksum integrity.
- [ ] Verification of Flyway migration schema consistency (`flyway_schema_history`).
- [ ] Verification of account balance sums matching transaction debit/credit totals.
