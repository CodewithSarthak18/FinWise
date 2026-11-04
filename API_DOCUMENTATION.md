# FinWise REST API Documentation

**Base URL**: `https://api.finwise.local/api/v1`  
**Protocol**: HTTPS / JSON  
**Authentication**: Bearer JWT (`Authorization: Bearer <accessToken>`)  
**Standard Response Envelope**:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

---

## 1. Authentication & Session Management

### Register User
- **Endpoint**: `POST /api/v1/auth/register`
- **Auth Required**: None
- **Request Body**:
  ```json
  {
    "email": "rahul.verma@example.com",
    "password": "SecurePassword#2026",
    "firstName": "Rahul",
    "lastName": "Verma",
    "currencyCode": "INR"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "success": true,
    "message": "User registered successfully",
    "data": {
      "tokenType": "Bearer",
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "4c9e66...",
      "expiresIn": 900000,
      "user": {
        "id": 1,
        "email": "rahul.verma@example.com",
        "firstName": "Rahul",
        "lastName": "Verma",
        "currencyCode": "INR"
      }
    }
  }
  ```

### Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Auth Required**: None
- **Request Body**:
  ```json
  {
    "email": "rahul.verma@example.com",
    "password": "SecurePassword#2026"
  }
  ```
- **Response**: `200 OK` (Returns same payload structure as Register).

### Refresh Token
- **Endpoint**: `POST /api/v1/auth/refresh-token`
- **Auth Required**: None
- **Request Body**:
  ```json
  {
    "refreshToken": "4c9e66..."
  }
  ```
- **Response**: `200 OK` (Returns new access token and rotated refresh token).

### Logout
- **Endpoint**: `POST /api/v1/auth/logout`
- **Auth Required**: Yes
- **Request Body**:
  ```json
  {
    "refreshToken": "4c9e66..."
  }
  ```
- **Response**: `200 OK`

---

## 2. User Profile

### Get Current User Profile
- **Endpoint**: `GET /api/v1/users/me`
- **Auth Required**: Yes
- **Response**: `200 OK`

### Change Password
- **Endpoint**: `PUT /api/v1/users/me/password`
- **Auth Required**: Yes
- **Request Body**:
  ```json
  {
    "currentPassword": "SecurePassword#2026",
    "newPassword": "NewStrongPassword#2027"
  }
  ```
- **Response**: `200 OK` (Revokes all active refresh tokens).

---

## 3. Account Management

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/accounts` | List all active accounts for user |
| `POST` | `/api/v1/accounts` | Create new bank, cash, or credit account |
| `GET` | `/api/v1/accounts/{id}` | Get account details by ID |
| `PUT` | `/api/v1/accounts/{id}` | Update account metadata |
| `DELETE` | `/api/v1/accounts/{id}` | Soft-delete account |

---

## 4. Categories

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/categories` | List system & user custom categories |
| `POST` | `/api/v1/categories` | Create custom category or subcategory |
| `GET` | `/api/v1/categories/{id}` | Get category by ID |
| `PUT` | `/api/v1/categories/{id}` | Update category |
| `DELETE` | `/api/v1/categories/{id}` | Delete category (non-system only) |

---

## 5. Transactions & Ledger

### Add Transaction
- **Endpoint**: `POST /api/v1/transactions`
- **Auth Required**: Yes
- **Request Body**:
  ```json
  {
    "accountId": 1,
    "categoryId": 5,
    "transactionType": "EXPENSE",
    "amount": "4500.00",
    "transactionDate": "2026-11-03T10:30:00Z",
    "merchantName": "Reliance Fresh",
    "paymentMode": "UPI",
    "notes": "Weekly household groceries"
  }
  ```
- **Response**: `201 Created`

### Query Transactions
- **Endpoint**: `GET /api/v1/transactions`
- **Query Params**:
  - `page`: Page index (default: `0`)
  - `size`: Page size (default: `20`)
  - `startDate`, `endDate`: ISO-8601 Date filtering
  - `categoryId`: Filter by category
  - `accountId`: Filter by account
  - `type`: `INCOME`, `EXPENSE`, or `REFUND`
  - `search`: Search keyword for merchant / notes
  - `sort`: Sort field and direction (`transactionDate,desc`)

### Refund Transaction
- **Endpoint**: `POST /api/v1/transactions/{id}/refund`
- **Auth Required**: Yes
- **Request Body**:
  ```json
  {
    "amount": "1200.00",
    "reason": "Item returned damaged"
  }
  ```
- **Response**: `200 OK` (Credits account, links parent transaction).

---

## 6. Transfers (Double-Entry)

### Execute Transfer
- **Endpoint**: `POST /api/v1/transfers`
- **Auth Required**: Yes
- **Request Body**:
  ```json
  {
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": "15000.00",
    "transferFee": "0.00",
    "transferDate": "2026-11-03T11:00:00Z",
    "note": "Transfer to Emergency Fund"
  }
  ```
- **Response**: `201 Created`

### Reverse Transfer
- **Endpoint**: `DELETE /api/v1/transfers/{id}`
- **Auth Required**: Yes
- **Response**: `200 OK` (Restores both account balances and removes debit/credit records).

---

## 7. Budgets & Threshold Alerts

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/budgets` | Get budgets for month/year |
| `POST` | `/api/v1/budgets` | Set category monthly spending cap |
| `GET` | `/api/v1/budgets/summary` | Real-time budget utilization & alert status |
| `PUT` | `/api/v1/budgets/{id}` | Update budget limit |
| `DELETE` | `/api/v1/budgets/{id}` | Delete budget |

---

## 8. Savings Goals

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/savings-goals` | List all savings goals & progress |
| `POST` | `/api/v1/savings-goals` | Create goal with target amount & date |
| `POST` | `/api/v1/savings-goals/{id}/contribute` | Record progress contribution |
| `DELETE` | `/api/v1/savings-goals/{id}` | Delete savings goal |

---

## 9. Recurring Transactions

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/recurring` | List recurring payment rules |
| `POST` | `/api/v1/recurring` | Create recurring rule (Daily, Weekly, Monthly) |
| `POST` | `/api/v1/recurring/{id}/pause` | Pause automated execution |
| `POST` | `/api/v1/recurring/{id}/resume` | Resume recurrence |
| `DELETE` | `/api/v1/recurring/{id}` | Delete recurring rule |

---

## 10. Financial Analytics & Smart Insights

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/summary` | Net savings, savings rate, total income/expense |
| `GET` | `/api/v1/analytics/trends` | 6-month historical cash flow trends |
| `GET` | `/api/v1/analytics/category-breakdown` | Category spending shares |
| `GET` | `/api/v1/analytics/needs-wants` | 50/30/20 Needs vs Wants breakdown |
| `GET` | `/api/v1/analytics/health-score` | Financial health score (0-100) & metrics |
| `GET` | `/api/v1/analytics/insights` | Rule-based smart actionable insights |
| `GET` | `/api/v1/analytics/subscriptions` | AI/rule-detected recurring subscriptions |
| `POST` | `/api/v1/analytics/what-if` | Simulate spending reduction scenarios |

---

## 11. Reports & Financial Exports

| Method | Endpoint | Format | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/reports/monthly` | JSON | Monthly financial summary & breakdowns |
| `GET` | `/api/v1/reports/annual` | JSON | Annual fiscal report |
| `GET` | `/api/v1/reports/categories` | JSON | Category-wise spending report |
| `GET` | `/api/v1/reports/export/pdf` | `application/pdf` | Formatted PDF export in INR (`Rs.`) |
| `GET` | `/api/v1/reports/export/csv` | `text/csv` | RFC-4180 compliant CSV export in INR |

---

## 12. Health & System Observability

- **`GET /api/v1/health`**: Public health status returning DB connectivity, JVM memory, and uptime.
- **`GET /actuator/health`**: Spring Boot Actuator standard health check.
- **`GET /actuator/health/liveness`**: Kubernetes / Docker liveness probe.
- **`GET /actuator/health/readiness`**: Kubernetes / Docker readiness probe.
