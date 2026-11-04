# FinWise Production Deployment & Operations Guide

This guide provides end-to-end instructions for deploying, securing, and maintaining the **FinWise Personal Finance Manager** platform in production.

---

## 1. GitHub & Version Control Preparation

Before pushing code or deploying to a production server, verify repository hygiene to prevent accidental data leaks or broken builds.

### 1.1. Secrets & Credentials Exclusion Checklist
- [x] **No hardcoded secrets**: Ensure no database passwords, JWT signing secrets, API keys, or private certificates exist in Git history or tracked files.
- [x] **`.gitignore` enforcement**: Confirm that `.gitignore` excludes all `.env*` files (except `.env.example`), `*.pem`, `*.key`, `*.jks`, `*.sql`, `*.dump`, and `target/`/`dist/` directories.
- [x] **Check untracked and staged files**:
  ```bash
  git status
  ```
  Ensure `.env` is **never** listed under staged changes.

### 1.2. Pre-Push Quality Gate
Always verify that both backend test suites and frontend production bundles succeed cleanly before pushing to your production branch:
```bash
# 1. Run all 106 backend automated tests
mvn test

# 2. Run frontend TypeScript type-checking and bundling
cd frontend && npm run build && cd ..
```

### 1.3. Branching Strategy
- **`main`**: Protected branch reserved strictly for production-ready, audited releases.
- **`develop` / Feature Branches**: Used for ongoing development, peer reviews, and automated CI test pipelines.

---

## 2. Production Environment Variables Reference

Configure these variables in your server's `.env` file (copied from `.env.example`).

| Variable Name | Required | Default / Fallback | Description | Secret Generation Command |
| :--- | :---: | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` | Activates production profile with strict JPA validation and logging | *Static value* (`prod`) |
| `PORT` | Yes | `8080` | Internal HTTP port for the Spring Boot backend service | *Static value* (`8080`) |
| `DB_HOST` | Yes | `mysql` | MySQL hostname (e.g., Aiven `mysql-xxxx-yourorg.aivencloud.com`) | *Static value* |
| `DB_PORT` | Yes | `3306` | MySQL port (e.g., Aiven `25060` or default `3306`) | *Static integer* |
| `DB_NAME` | Yes | `finwise_db` | Database name (e.g., Aiven `defaultdb` or `finwise_db`) | *Static value* |
| `DB_USERNAME` | Yes | `finwise_user` | Database user (e.g., Aiven `avnadmin` or dedicated user) | *Static value* |
| `DB_PASSWORD` | Yes | *(None)* | Strong database password (copied securely from Aiven console) | Managed in DB console |
| `DB_SSL_MODE` | No | `REQUIRED` | SSL mode for MySQL JDBC (`REQUIRED`, `VERIFY_CA`, `VERIFY_IDENTITY`) | *Static value* (`REQUIRED`) |
| `DB_JDBC_URL` | No | *(Auto-built)* | Optional full JDBC URL override (if not using individual DB_* variables) | *Static value* |
| `MYSQL_ROOT_PASSWORD` | No | *(None)* | MySQL root password (used only for local Docker MySQL container) | `openssl rand -base64 24` |
| `JWT_SECRET` | Yes | *(None)* | High-entropy 256/384/512-bit HMAC key (min 64 hex chars) | `openssl rand -hex 64` |
| `JWT_ACCESS_EXPIRATION_MS` | No | `900000` | Access token lifespan in milliseconds (15 minutes) | *Static integer* |
| `JWT_REFRESH_EXPIRATION_MS`| No | `604800000` | Refresh token lifespan in milliseconds (7 days) | *Static integer* |
| `JWT_ISSUER` | No | `FinWiseApp` | Registered issuer claim (`iss`) embedded in tokens | *Static value* |
| `CORS_ALLOWED_ORIGINS` | Yes | `https://localhost` | Comma-separated list of allowed frontend origins (no trailing slash) | E.g. `https://finwise.yourdomain.com` |
| `DOMAIN_NAME` | Yes | `finwise.yourdomain.com` | Production FQDN for Nginx SSL certificate matching | E.g. `finwise.yourdomain.com` |
| `VITE_API_URL` | No | `/api/v1` | Frontend API base path (when reverse proxied, use `/api/v1`) | *Static value* (`/api/v1`) |
| `SCHEDULER_ENABLED` | No | `true` | Enables background recurring transaction scheduler | `true` or `false` |
| `LOG_LEVEL_ROOT` | No | `INFO` | Root logging level | `INFO` or `WARN` |
| `LOG_LEVEL_FINWISE` | No | `INFO` | FinWise application logging level | `INFO` |

---

## 3. Production MySQL Database Provisioning & Aiven Hosted MySQL Setup

FinWise supports cloud-hosted managed databases like **Aiven for MySQL**, AWS RDS, and Azure Database for MySQL, as well as containerized MySQL instances.

### 3.1. Aiven for MySQL Setup & Connection Guide

Aiven provides managed, highly available MySQL clusters with mandatory SSL/TLS encryption.

#### Step 1: Obtain Connection Details from Aiven Console
1. Log in to [Aiven Console](https://console.aiven.io/).
2. Navigate to your **MySQL Service Overview** page.
3. Under **Connection information**, identify:
   - **Host**: e.g., `mysql-xxxx-yourorg.aivencloud.com`
   - **Port**: e.g., `25060` (or your assigned service port)
   - **User**: e.g., `avnadmin`
   - **Password**: Copy your secret password (never commit this)
   - **Database Name**: `defaultdb` (or create a dedicated database named `finwise_db` in the **Databases** tab)
   - **Access**: Ensure public access is allowed or configure IP filter rules for your application server's static IP.

#### Step 2: Configure Environment Variables in `.env`
Set the database parameters in your production `.env` file:
```bash
# Aiven MySQL Connection Parameters
DB_HOST=mysql-xxxx-yourorg.aivencloud.com
DB_PORT=25060
DB_NAME=defaultdb
DB_USERNAME=avnadmin
DB_PASSWORD=your_aiven_password_here
DB_SSL_MODE=REQUIRED
```

The Spring Boot backend automatically constructs the compliant JDBC URL:
```text
jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslMode=${DB_SSL_MODE:REQUIRED}&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
```

#### Step 3: SSL/TLS Encryption Modes
Aiven strictly **requires** encrypted SSL/TLS connections:
- **`sslMode=REQUIRED` (Default & Recommended)**: Enforces end-to-end TLS encryption on all data transmitted between FinWise and Aiven MySQL.
- **`sslMode=VERIFY_IDENTITY` or `sslMode=VERIFY_CA` (Strict CA Certificate Verification)**:
  If your organization requires certificate authority verification against Aiven's Project CA:
  1. Download `ca.pem` from the Aiven MySQL Service Overview page.
  2. Import `ca.pem` into a Java keystore:
     ```bash
     keytool -importcert -alias aiven -file ca.pem -keystore aiven-truststore.jks -storepass changeit -noprompt
     ```
  3. Supply JVM truststore flags or append to `DB_JDBC_URL`:
     ```text
     &sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:/path/to/aiven-truststore.jks&trustCertificateKeyStorePassword=changeit
     ```

### 3.2. Manual MySQL Host / Self-Hosted Provisioning
If using a self-hosted MySQL instance instead of Aiven:

```sql
-- 1. Create database with full Unicode & Emoji support
CREATE DATABASE IF NOT EXISTS finwise_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create least-privilege dedicated application user
CREATE USER IF NOT EXISTS 'finwise_user'@'%'
    IDENTIFIED BY 'YOUR_GENERATED_STRONG_DB_PASSWORD';

-- 3. Grant schema permissions
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, LOCK TABLES, EXECUTE
    ON finwise_db.* TO 'finwise_user'@'%';

FLUSH PRIVILEGES;
```

### 3.3. Production InnoDB Engine Tuning (`docker/mysql/my.cnf`):
- `innodb_buffer_pool_size = 512M` (or 50-70% of available RAM on dedicated DB hosts).
- `innodb_flush_log_at_trx_commit = 1` for absolute financial transaction ACID durability.
- `sql_mode = STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION`.
- `binlog_format = ROW` with 7-day expiration for point-in-time recovery.

---

## 4. Flyway Database Migrations

FinWise uses version-controlled Flyway database migrations (`src/main/resources/db/migration/`):
- `V1__init_schema.sql`: Creates core tables with foreign keys and composite indexes.
- `V2__seed_categories.sql`: Seeds system categories (`is_system = true`).
- `V3__sample_dev_data.sql`: Development seed data (skipped if schema is already baselined).

### Automatic Migration & Cloud Resilience on Startup:
The Spring Boot backend has Flyway auto-migration configured with cloud-resilient safeguards:
```yaml
spring.flyway:
  enabled: true
  baseline-on-migrate: true
  baseline-version: 0
  validate-on-migrate: true
  clean-disabled: true       # Prevents accidental deletion of production tables
  connect-retries: 5         # Handles cloud database connection handshakes
  connect-retries-interval: 3 # Waits 3 seconds between retries
```
On startup, the backend automatically validates applied migrations and executes any pending migration scripts before accepting incoming API traffic.

### Database Health Probes:
The health of the database connection (including TLS handshakes) is verified continuously through:
- **Application Health Endpoint**: `GET /api/v1/health` (executes JDBC `isValid(2)` probe and reports database product name, version, and status).
- **Spring Actuator Probes**: `GET /actuator/health/readiness` and `GET /actuator/health/liveness` (reports database component state for container orchestrators).

### Manual Migration Verification (Optional):
You can check migration history directly in MySQL:
```sql
SELECT installed_rank, version, description, type, script, execution_time, success
FROM finwise_db.flyway_schema_history
ORDER BY installed_rank ASC;
```

---

## 5. Deployment Architectures

### Option A: Turnkey Docker Compose (Recommended)
This approach launches MySQL, the backend, and the frontend on a single host.

1. **Clone repository and configure `.env`**:
   ```bash
   git clone https://github.com/your-username/FinWise.git /opt/finwise
   cd /opt/finwise
   cp .env.example .env
   chmod 600 .env
   # Edit .env with your generated secrets
   nano .env
   ```

2. **Launch the stack**:
   ```bash
   docker compose up -d --build
   ```

3. **Verify running containers**:
   ```bash
   docker compose ps
   ```
   Both `finwise-backend` and `finwise-frontend` will report `(healthy)`.

---

### Option B: Production HTTPS / SSL Deployment with Certbot
This option attaches Nginx SSL termination with TLS 1.2/1.3, 2-year HSTS, and automated Certbot certificate renewal.

1. **Launch production compose stack**:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
   ```

2. **Request initial Let's Encrypt certificate**:
   ```bash
   docker compose run --rm certbot certonly --webroot -w /var/www/certbot \
       -d finwise.yourdomain.com \
       --email admin@yourdomain.com \
       --agree-tos --no-eff-email
   ```

3. **Reload Nginx SSL proxy**:
   ```bash
   docker compose exec proxy nginx -s reload
   ```

---

### Option C: Bare-Metal / Systemd Deployment (Without Docker)

#### 1. Backend Systemd Service (`/etc/systemd/system/finwise-backend.service`)
```ini
[Unit]
Description=FinWise Personal Finance Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=finwise
Group=finwise
WorkingDirectory=/opt/finwise
EnvironmentFile=/opt/finwise/.env
ExecStart=/usr/bin/java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dspring.profiles.active=prod -jar /opt/finwise/target/finwise-backend-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```
Start and enable the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now finwise-backend
sudo systemctl status finwise-backend
```

#### 2. Frontend Nginx Configuration (`/etc/nginx/sites-available/finwise`)
```nginx
server {
    listen 80;
    server_name finwise.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name finwise.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/finwise.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/finwise.yourdomain.com/privkey.pem;

    root /opt/finwise/frontend/dist;
    index index.html;

    # Static Assets Caching
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API Reverse Proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    # SPA Routing Fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 6. Production CORS Architecture

CORS is dynamically managed by [`CorsConfig.java`](file:///C:/Users/sarth/Downloads/FinWise/src/main/java/com/finwise/config/CorsConfig.java) using the `CORS_ALLOWED_ORIGINS` environment variable:

```yaml
finwise:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: Authorization,Content-Type,Accept,Origin,X-Requested-With,Idempotency-Key
    allow-credentials: true
    max-age: 3600
```

- **Single Domain (Recommended)**: If the frontend and backend are served behind the same domain via Nginx (e.g. `https://finwise.yourdomain.com` with API requests to `/api/v1/*`), requests are same-origin and browser CORS restrictions do not apply.
- **Cross-Domain**: If the frontend is hosted on `https://app.finwise.in` and the API on `https://api.finwise.in`, configure:
  ```env
  CORS_ALLOWED_ORIGINS=https://app.finwise.in,https://finwise.in
  ```

---

## 7. Post-Deployment Smoke Tests

Execute these verification commands after deployment to confirm operational health:

### Test 1: Public Health Check
```bash
curl -s -X GET "https://finwise.yourdomain.com/api/v1/health" | jq .
```
**Expected Output**:
```json
{
  "success": true,
  "message": "Health check completed",
  "data": {
    "status": "UP",
    "application": "FinWise Personal Finance Backend",
    "version": "1.0.0",
    "environment": "prod",
    "components": {
      "database": {
        "status": "UP"
      }
    }
  }
}
```

### Test 2: User Registration
```bash
curl -s -X POST "https://finwise.yourdomain.com/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "smoke.test@finwise.in",
    "password": "SecurePassword#2026",
    "firstName": "Smoke",
    "lastName": "Tester",
    "currencyCode": "INR"
  }' | jq .
```
**Expected Output**: `201 Created` with `accessToken`, `refreshToken`, and user object.

### Test 3: User Login & JWT Token Retrieval
```bash
TOKEN=$(curl -s -X POST "https://finwise.yourdomain.com/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "smoke.test@finwise.in",
    "password": "SecurePassword#2026"
  }' | jq -r '.data.accessToken')

echo "Acquired Bearer Token: ${TOKEN:0:20}..."
```

### Test 4: Account Creation
```bash
ACC_ID=$(curl -s -X POST "https://finwise.yourdomain.com/api/v1/accounts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "HDFC Primary Checking",
    "accountType": "SAVINGS",
    "initialBalance": "50000.00",
    "currencyCode": "INR"
  }' | jq -r '.data.id')

echo "Created Account ID: $ACC_ID"
```

### Test 5: Expense Transaction Creation
```bash
TX_ID=$(curl -s -X POST "https://finwise.yourdomain.com/api/v1/transactions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"accountId\": $ACC_ID,
    \"categoryId\": 1,
    \"transactionType\": \"EXPENSE\",
    \"amount\": \"2500.00\",
    \"transactionDate\": \"2026-11-03T10:00:00Z\",
    \"merchantName\": \"Supermarket Groceries\",
    \"paymentMode\": \"UPI\"
  }" | jq -r '.data.id')

echo "Recorded Expense ID: $TX_ID"
```

### Test 6: Expense Refund Processing
```bash
curl -s -X POST "https://finwise.yourdomain.com/api/v1/transactions/$TX_ID/refund" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "500.00",
    "reason": "Overcharge partial refund"
  }' | jq .
```
**Expected Output**: `200 OK` with generated `REFUND` transaction and credited balance.

### Test 7: CSV Financial Report Generation
```bash
curl -s -X GET "https://finwise.yourdomain.com/api/v1/reports/export/csv?type=MONTHLY&year=2026&month=11" \
  -H "Authorization: Bearer $TOKEN" \
  --output november_2026_report.csv

head -n 10 november_2026_report.csv
```
**Expected Output**: Valid RFC-4180 CSV containing Indian Rupee formatted numbers (`Rs.`).

---

## 8. Rollback Procedures & Troubleshooting

### Emergency Rollback:
```bash
# 1. Checkout previous stable release tag or commit
git checkout v1.0.0

# 2. Rebuild and restart containers
docker compose up -d --build --no-deps backend frontend
```

### Database Restore from Hot Backup:
```bash
./scripts/restore.sh ./backups/finwise_backup_20261103_023000.sql.gz
```
