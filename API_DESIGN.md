# FinWise - Smart Personal Finance Manager
## RESTful API Design Specification

---

### 1. Architectural Standards & Conventions

1. **Protocol & Versioning**:
   * Base URI: `https://api.finwise.local/api/v1`
   * URI versioning is strictly enforced (`/v1`). Any non-backward-compatible change will be released under `/v2`.

2. **Content Negotiation & Encoding**:
   * `Content-Type: application/json; charset=utf-8`
   * `Accept: application/json`

3. **Monetary Precision in Payloads**:
   * All monetary values in JSON are formatted as exact numeric strings (e.g., `"12450.50"`) or standard JSON numbers without exponents. Strings are preferred in DTOs to avoid JavaScript floating-point parsing loss.
   * Standard currency ISO code: `"INR"`.
   * Frontend display formatter helper transforms this into `₹12,450.50`.

4. **Idempotency for Financial Mutations**:
   * All transaction creation and transfer endpoints require an `Idempotency-Key: <UUIDv4>` header.
   * If a client retries due to network failure, the backend recognizes the key from Redis/database and returns the cached result without duplicate debit or credit.

5. **Pagination, Filtering & Sorting**:
   * Spring Data standard query params:
     * `page`: 0-indexed page number (default: `0`).
     * `size`: page size (default: `20`, max: `100`).
     * `sort`: property and direction (e.g., `sort=transactionDate,desc`).
   * Standard pagination envelope format:
     ```json
     {
       "content": [ ... ],
       "pageNumber": 0,
       "pageSize": 20,
       "totalElements": 154,
       "totalPages": 8,
       "isLast": false
     }
     ```

6. **Standardized RFC 7807 Problem Details for Error Responses**:
   * All 4xx and 5xx responses return `application/problem+json`:
     ```json
     {
       "type": "https://api.finwise.com/errors/insufficient-funds",
       "title": "Insufficient Funds",
       "status": 400,
       "detail": "Account 'HDFC Salary' has balance ₹4,500.00, which is insufficient for transaction amount ₹10,000.00",
       "instance": "/api/v1/transactions",
       "timestamp": "2026-11-03T10:15:30.124Z",
       "errorCode": "FIN_400_INSUFFICIENT_FUNDS",
       "validationErrors": null
     }
     ```

---

### 2. Comprehensive Endpoint Catalog

#### 2.1. Authentication & Security (`/api/v1/auth`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register new user account | Anonymous |
| `POST` | `/api/v1/auth/login` | Authenticate user & issue JWT pair | Anonymous |
| `POST` | `/api/v1/auth/refresh-token` | Exchange valid refresh token for new access token | Anonymous |
| `POST` | `/api/v1/auth/logout` | Invalidate refresh token and blacklist access token | Authenticated |
| `POST` | `/api/v1/auth/forgot-password`| Trigger password reset email with secure OTP | Anonymous |
| `POST` | `/api/v1/auth/reset-password` | Set new password using verified token/OTP | Anonymous |

##### Request: Register User
```json
// POST /api/v1/auth/register
{
  "firstName": "Arun",
  "lastName": "Sharma",
  "email": "arun.sharma@example.in",
  "password": "SecurePassword#2026",
  "phoneNumber": "+919876543210",
  "currencyCode": "INR"
}
```

##### Response: Auth Tokens Response (200 OK)
```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
  "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "expiresInSeconds": 900,
  "user": {
    "id": 101,
    "email": "arun.sharma@example.in",
    "firstName": "Arun",
    "lastName": "Sharma",
    "currencyCode": "INR",
    "roles": ["ROLE_USER"]
  }
}
```

---

#### 2.2. User Management & Profile (`/api/v1/users`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | Get currently authenticated user profile | Authenticated |
| `PUT` | `/api/v1/users/me` | Update personal details & preferences | Authenticated |
| `PUT` | `/api/v1/users/me/password` | Change password with current password verification | Authenticated |

---

#### 2.3. Accounts Management (`/api/v1/accounts`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/accounts` | List all accounts for user with balances | Authenticated |
| `POST` | `/api/v1/accounts` | Create a new financial account | Authenticated |
| `GET` | `/api/v1/accounts/{id}` | Get account details and summary statistics | Authenticated |
| `PUT` | `/api/v1/accounts/{id}` | Update account properties (name, limit) | Authenticated |
| `DELETE` | `/api/v1/accounts/{id}` | Soft-delete account (checks for non-zero balance) | Authenticated |

##### Request: Create Account
```json
// POST /api/v1/accounts
{
  "accountName": "HDFC Savings Main",
  "accountType": "SAVINGS",
  "institutionName": "HDFC Bank",
  "accountNumberMask": "XXXX-XXXX-4589",
  "initialBalance": "50000.00",
  "currencyCode": "INR",
  "creditLimit": "0.00"
}
```

##### Response: Account Details (201 Created)
```json
{
  "id": 12,
  "accountName": "HDFC Savings Main",
  "accountType": "SAVINGS",
  "institutionName": "HDFC Bank",
  "accountNumberMask": "XXXX-XXXX-4589",
  "currentBalance": "50000.00",
  "formattedBalance": "₹50,000.00",
  "currencyCode": "INR",
  "creditLimit": "0.00",
  "status": "ACTIVE",
  "createdAt": "2026-11-03T00:15:00Z",
  "updatedAt": "2026-11-03T00:15:00Z"
}
```

---

#### 2.4. Categories Management (`/api/v1/categories`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/categories` | Get hierarchical category tree (system + custom) | Authenticated |
| `POST` | `/api/v1/categories` | Create user custom category | Authenticated |
| `PUT` | `/api/v1/categories/{id}` | Update category name/icon/color (custom only) | Authenticated |
| `DELETE` | `/api/v1/categories/{id}` | Soft-delete category | Authenticated |

##### Response: Hierarchical Categories
```json
// GET /api/v1/categories?type=EXPENSE
[
  {
    "id": 1,
    "name": "Housing & Rent",
    "type": "EXPENSE",
    "icon": "home",
    "colorCode": "#EF4444",
    "isSystem": true,
    "subcategories": []
  },
  {
    "id": 2,
    "name": "Transportation & Fuel",
    "type": "EXPENSE",
    "icon": "car",
    "colorCode": "#3B82F6",
    "isSystem": true,
    "subcategories": [
      {
        "id": 105,
        "name": "Metro & Public Transit",
        "type": "EXPENSE",
        "icon": "train",
        "colorCode": "#60A5FA",
        "isSystem": false
      }
    ]
  }
]
```

---

#### 2.5. Transactions Engine (`/api/v1/transactions`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/transactions` | Paginated search & filtering of transactions | Authenticated |
| `POST` | `/api/v1/transactions` | Record new Income or Expense transaction | Authenticated |
| `GET` | `/api/v1/transactions/{id}` | Get single transaction detail | Authenticated |
| `PUT` | `/api/v1/transactions/{id}` | Update transaction and recalculate account balance | Authenticated |
| `DELETE` | `/api/v1/transactions/{id}` | Delete transaction and roll back account balance | Authenticated |
| `POST` | `/api/v1/transactions/bulk` | Bulk import transactions (from bank CSV) | Authenticated |
| `GET` | `/api/v1/transactions/export` | Export filtered transactions to CSV / Excel | Authenticated |

##### Request: Create Transaction
```json
// POST /api/v1/transactions
// Header: Idempotency-Key: c9b2f28b-1e24-4db6-9467-33b668d29b28
{
  "accountId": 12,
  "categoryId": 2,
  "transactionType": "EXPENSE",
  "amount": "2450.00",
  "currencyCode": "INR",
  "transactionDate": "2026-11-03T18:30:00Z",
  "merchantName": "Indian Oil Fuel Station",
  "description": "Monthly petrol refill for car",
  "paymentMode": "UPI",
  "referenceNumber": "UPI/20261103/987123456"
}
```

##### Query Parameters for `GET /api/v1/transactions`:
* `page`: integer (0-indexed)
* `size`: integer (default: 20)
* `sort`: string (`transactionDate,desc`)
* `startDate`: `YYYY-MM-DD`
* `endDate`: `YYYY-MM-DD`
* `accountId`: integer
* `categoryId`: integer
* `transactionType`: `INCOME` | `EXPENSE` | `TRANSFER_DEBIT` | `TRANSFER_CREDIT`
* `search`: string (matches merchant name or description)
* `minAmount`: decimal string
* `maxAmount`: decimal string

---

#### 2.6. Account Transfers (`/api/v1/transfers`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/transfers` | Transfer funds atomically between accounts | Authenticated |
| `GET` | `/api/v1/transfers` | Paginated list of past account transfers | Authenticated |
| `GET` | `/api/v1/transfers/{id}` | Get transfer details with paired debit/credit | Authenticated |

##### Request: Execute Transfer
```json
// POST /api/v1/transfers
// Header: Idempotency-Key: e4a19c72-91f3-4a11-8e01-1e9bfdbfae82
{
  "fromAccountId": 12,
  "toAccountId": 15,
  "amount": "15000.00",
  "transferFee": "0.00",
  "transferDate": "2026-11-03T19:00:00Z",
  "note": "Transfer to Zerodha Demat trading account"
}
```

##### Response: Transfer Result (201 Created)
```json
{
  "transferId": 48,
  "fromAccountId": 12,
  "fromAccountName": "HDFC Savings Main",
  "toAccountId": 15,
  "toAccountName": "Zerodha Demat Account",
  "amount": "15000.00",
  "formattedAmount": "₹15,000.00",
  "transferFee": "0.00",
  "debitTransactionId": 3201,
  "creditTransactionId": 3202,
  "transferDate": "2026-11-03T19:00:00Z",
  "status": "COMPLETED"
}
```

---

#### 2.7. Budgets & Thresholds (`/api/v1/budgets`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/budgets/current` | Get active budget with real-time spend tracking | Authenticated |
| `POST` | `/api/v1/budgets` | Define a new monthly or custom budget | Authenticated |
| `GET` | `/api/v1/budgets` | List historical budgets | Authenticated |
| `GET` | `/api/v1/budgets/{id}` | Get specific budget with category allocations | Authenticated |
| `PUT` | `/api/v1/budgets/{id}` | Update budget limits and category splits | Authenticated |
| `DELETE` | `/api/v1/budgets/{id}` | Delete a budget plan | Authenticated |

##### Response: Current Active Budget with Burn Rate
```json
// GET /api/v1/budgets/current
{
  "id": 5,
  "name": "November 2026 Monthly Budget",
  "periodType": "MONTHLY",
  "startDate": "2026-11-01",
  "endDate": "2026-11-30",
  "totalBudgetAmount": "80000.00",
  "formattedTotalBudget": "₹80,000.00",
  "totalSpent": "24500.00",
  "formattedTotalSpent": "₹24,500.00",
  "utilizationPercentage": 30.63,
  "notificationThreshold": 80.00,
  "isThresholdExceeded": false,
  "categories": [
    {
      "categoryId": 2,
      "categoryName": "Transportation & Fuel",
      "allocatedAmount": "8000.00",
      "spentAmount": "2450.00",
      "remainingAmount": "5550.00",
      "utilizationPercentage": 30.63,
      "colorCode": "#3B82F6"
    },
    {
      "categoryId": 3,
      "categoryName": "Food & Dining Out",
      "allocatedAmount": "12000.00",
      "spentAmount": "10800.00",
      "remainingAmount": "1200.00",
      "utilizationPercentage": 90.00,
      "colorCode": "#F97316"
    }
  ]
}
```

---

#### 2.8. Financial Goals & Targets (`/api/v1/goals`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/goals` | List all savings goals with progress percentage | Authenticated |
| `POST` | `/api/v1/goals` | Create a new savings goal | Authenticated |
| `GET` | `/api/v1/goals/{id}` | Get goal details and contribution history | Authenticated |
| `PUT` | `/api/v1/goals/{id}` | Update goal target or deadline | Authenticated |
| `POST` | `/api/v1/goals/{id}/contribute`| Allocate funds from account towards goal | Authenticated |
| `DELETE` | `/api/v1/goals/{id}` | Delete goal | Authenticated |

##### Request: Contribute to Goal
```json
// POST /api/v1/goals/3/contribute
{
  "accountId": 12,
  "amount": "10000.00",
  "note": "Monthly SIP transfer to Emergency Fund"
}
```

---

#### 2.9. Recurring Rules & Subscriptions (`/api/v1/recurring`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/recurring` | List active recurring transactions & bills | Authenticated |
| `POST` | `/api/v1/recurring` | Create recurring rule (Daily, Weekly, Monthly) | Authenticated |
| `PUT` | `/api/v1/recurring/{id}` | Update recurring parameters | Authenticated |
| `PATCH` | `/api/v1/recurring/{id}/toggle` | Pause or activate recurring rule | Authenticated |
| `POST` | `/api/v1/recurring/{id}/execute-now` | Manually trigger execution of recurring rule | Authenticated |

---

#### 2.10. Analytics & Financial Intelligence (`/api/v1/analytics`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/analytics/dashboard-summary` | Summary KPIs (Total Net Worth, Inflow, Outflow, Savings Rate) | Authenticated |
| `GET` | `/api/v1/analytics/cash-flow` | Inflow vs Outflow by month/week (Recharts Bar Chart) | Authenticated |
| `GET` | `/api/v1/analytics/category-breakdown` | Expense distribution by category (Recharts Donut Chart) | Authenticated |
| `GET` | `/api/v1/analytics/net-worth-trend` | Net worth progression over 6/12/24 months (Recharts Area Chart) | Authenticated |

##### Response: Dashboard Summary KPIs
```json
// GET /api/v1/analytics/dashboard-summary
{
  "netWorth": "1485000.00",
  "formattedNetWorth": "₹14,85,000.00",
  "currentMonthIncome": "125000.00",
  "formattedCurrentMonthIncome": "₹1,25,000.00",
  "currentMonthExpense": "48200.00",
  "formattedCurrentMonthExpense": "₹48,200.00",
  "savingsRatePercentage": 61.44,
  "activeAccountsCount": 4,
  "activeBudgetsCount": 1,
  "budgetUtilizationPercentage": 60.25
}
```

##### Response: Cash Flow Time Series for Recharts
```json
// GET /api/v1/analytics/cash-flow?months=6
[
  {
    "monthYear": "2026-06",
    "displayLabel": "Jun 2026",
    "income": 120000.00,
    "expense": 45000.00,
    "netSavings": 75000.00
  },
  {
    "monthYear": "2026-07",
    "displayLabel": "Jul 2026",
    "income": 120000.00,
    "expense": 52000.00,
    "netSavings": 68000.00
  },
  {
    "monthYear": "2026-08",
    "displayLabel": "Aug 2026",
    "income": 135000.00,
    "expense": 48000.00,
    "netSavings": 87000.00
  }
]
```

---

### 3. Rate Limiting & HTTP Security Headers

1. **Rate Limiting Policy**:
   * Token Bucket algorithm implemented via Bucket4j:
     * Unauthenticated endpoints (`/api/v1/auth/**`): 10 requests per minute per IP.
     * Authenticated endpoints (`/api/v1/**`): 120 requests per minute per user ID.
   * Standard response headers returned on all requests:
     * `X-RateLimit-Limit: 120`
     * `X-RateLimit-Remaining: 114`
     * `X-RateLimit-Reset: 1798934500`

2. **Security Headers**:
   ```http
   Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
   X-Content-Type-Options: nosniff
   X-Frame-Options: DENY
   X-XSS-Protection: 1; mode=block
   Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';
   ```
