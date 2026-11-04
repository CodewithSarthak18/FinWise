# FinWise Local & Production Setup Guide

This guide provides step-by-step instructions for running FinWise locally on any developer's machine or deploying it to a production Linux host.

---

## Prerequisites

| Requirement | Version | Required For |
| :--- | :--- | :--- |
| **Docker Engine & Docker Compose** | 24.0+ / Compose v2 | Containerized Deployment (Recommended) |
| **Java JDK** | 21 (LTS) | Local Backend Development |
| **Node.js & npm** | Node 20+, npm 10+ | Local Frontend Development |
| **MySQL Server** | 8.0+ | Local Database (if not using Docker) |

---

## Path 1: One-Command Docker Compose (Fastest & Recommended)

This path launches MySQL 8.0, compiles and starts the Spring Boot backend with Flyway migrations, and starts the Nginx-hosted React frontend.

### 1. Clone & Configure Environment
```bash
git clone https://github.com/your-username/FinWise.git
cd FinWise

# Create environment file from template
cp .env.example .env
```
*(On Windows PowerShell, use `Copy-Item .env.example .env`)*

### 2. Launch the Stack
```bash
docker compose up -d --build
```

### 3. Verify System Health
Check container statuses:
```bash
docker compose ps
```
Both `finwise-backend` and `finwise-frontend` will report `(healthy)` within ~40 seconds.

### 4. Access the Application
- **Frontend Web Dashboard**: Open [http://localhost](http://localhost) in your browser.
- **Backend API Base**: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **Health Check**: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
- **Actuator Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 5. Stopping the Stack
```bash
docker compose down
# To stop and remove persistent database volumes:
docker compose down -v
```

---

## Path 2: Bare-Metal / Manual Local Development

Use this path if you want to run the backend and frontend separately in your IDE with hot-reloading.

### 1. Start MySQL 8.0
Ensure MySQL is running on port `3306`. Create the database and user:
```sql
CREATE DATABASE finwise_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'finwise_user'@'localhost' IDENTIFIED BY 'finwise_pass_2026';
GRANT ALL PRIVILEGES ON finwise_db.* TO 'finwise_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Run Backend (Spring Boot 3 + Java 21)
In the project root:
```bash
# On Linux/macOS
./mvnw spring-boot:run

# On Windows
tools\maven\bin\mvn.cmd spring-boot:run
```
Flyway will automatically initialize the database schema and seed default categories.
The backend will be live on `http://localhost:8080`.

### 3. Run Frontend (React + Vite)
In a separate terminal window:
```bash
cd frontend
npm install
npm run dev
```
The Vite development server will start at `http://localhost:5173`. Any calls to `/api/*` are automatically proxied to `http://localhost:8080`.

---

## Path 3: Production Cloud VM Deployment (Ubuntu / Debian)

### 1. Server Provisioning
Install Docker and Docker Compose:
```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
# Install Docker Engine & Docker Compose plugin
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

### 2. Clone Repository & Set Production Secrets
```bash
cd /opt
sudo git clone https://github.com/your-username/FinWise.git finwise
cd finwise
sudo cp .env.example .env
```
Edit `.env` using `sudo nano .env`:
- Set `DB_PASSWORD` to a strong 32-character password.
- Set `JWT_SECRET` to a 64-character hex string (`openssl rand -hex 64`).
- Set `DOMAIN_NAME` to your registered domain (e.g. `finwise.yourdomain.com`).
- Set `CORS_ALLOWED_ORIGINS` to `https://finwise.yourdomain.com`.

### 3. Launch with Production SSL Reverse Proxy
```bash
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### 4. Enable Automated Daily Database Backups
```bash
sudo chmod +x scripts/backup.sh scripts/restore.sh
# Add to root crontab:
(sudo crontab -l 2>/dev/null; echo "30 2 * * * cd /opt/finwise && ./scripts/backup.sh >> /var/log/finwise/backup-cron.log 2>&1") | sudo crontab -
```

---

## Running Automated Tests

To execute the entire 105+ test suite:
```bash
# Backend unit, integration, and security tests
mvn test

# Frontend TypeScript typechecking & bundle compilation
cd frontend && npm run build
```
