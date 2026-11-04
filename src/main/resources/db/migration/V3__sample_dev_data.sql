-- =============================================================================
-- FinWise Development Seed Data - V3__sample_dev_data.sql
-- Description: Realistic mock financial data for local development & testing
-- Note: All names, numbers, account numbers, and figures are strictly fictional
-- =============================================================================

-- 1. Insert Sample Mock Development Users (Password: DevPassword#2026)
-- BCrypt Hash: $2a$12$e5V/iR/5lK2U8B9gZc8Eke01xT28YvY45bQ28eQn7qV05rV3kQG7K
INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number, currency_code, is_active, version)
VALUES 
(1, 'rahul.verma@example.com', '$2a$12$e5V/iR/5lK2U8B9gZc8Eke01xT28YvY45bQ28eQn7qV05rV3kQG7K', 'Rahul', 'Verma', '+919876543210', 'INR', TRUE, 0),
(2, 'ananya.sen@example.com', '$2a$12$e5V/iR/5lK2U8B9gZc8Eke01xT28YvY45bQ28eQn7qV05rV3kQG7K', 'Ananya', 'Sen', '+919812345678', 'INR', TRUE, 0)
ON DUPLICATE KEY UPDATE first_name=VALUES(first_name);

-- 2. Assign ROLE_USER
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1), (2, 1)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- 3. Insert Sample Financial Accounts for User 1
INSERT INTO accounts (id, user_id, account_name, account_type, institution_name, account_number_mask, current_balance, initial_balance, currency_code, credit_limit, status, version)
VALUES
(1, 1, 'HDFC Salary Savings', 'SAVINGS', 'HDFC Bank', 'XXXX-XXXX-4589', 85400.50, 50000.00, 'INR', 0.00, 'ACTIVE', 0),
(2, 1, 'ICICI Coral Card', 'CREDIT_CARD', 'ICICI Bank', 'XXXX-XXXX-1122', 12450.00, 0.00, 'INR', 150000.00, 'ACTIVE', 0),
(3, 1, 'Zerodha Demat & Trading', 'INVESTMENT', 'Zerodha Broking', 'XXXX-DEMAT-78', 245000.00, 100000.00, 'INR', 0.00, 'ACTIVE', 0),
(4, 1, 'Paytm Wallet & UPI', 'WALLET_UPI', 'Paytm Payments Bank', 'XXXX-UPI-8899', 4250.75, 2000.00, 'INR', 0.00, 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE account_name=VALUES(account_name);

-- 4. Insert Sample Recurring Transactions (SIPs, Utilities, Subscriptions)
INSERT INTO recurring_transactions (id, user_id, account_id, category_id, title, amount, transaction_type, frequency, start_date, end_date, next_execution_date, auto_post, is_active, description, version)
VALUES
(1, 1, 1, 7, 'Nifty 50 Index Mutual Fund SIP', 15000.00, 'EXPENSE', 'MONTHLY', '2026-01-05', '2030-12-05', '2026-11-05', TRUE, TRUE, 'Automated wealth creation SIP', 0),
(2, 1, 2, 8, 'Netflix 4K Ultra Subscription', 649.00, 'EXPENSE', 'MONTHLY', '2026-02-15', NULL, '2026-11-15', TRUE, TRUE, 'Monthly streaming entertainment', 0),
(3, 1, 1, 5, 'Airtel Fiber Broadband Bill', 999.00, 'EXPENSE', 'MONTHLY', '2026-01-20', NULL, '2026-11-20', TRUE, TRUE, 'Home 300 Mbps internet plan', 0)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 5. Insert Sample Ledger Transactions for User 1
INSERT INTO transactions (id, user_id, account_id, category_id, recurring_transaction_id, transaction_type, amount, currency_code, transaction_date, merchant_name, description, payment_mode, reference_number, status)
VALUES
(1, 1, 1, 11, NULL, 'INCOME', 115000.00, 'INR', '2026-11-01 04:30:00', 'Tech Solutions Corp India', 'Monthly Tech Salary Credit', 'NEFT_RTGS_IMPS', 'CMS/20261101/SALARY/01', 'COMPLETED'),
(2, 1, 1, 1, NULL, 'EXPENSE', 22000.00, 'INR', '2026-11-02 05:15:00', 'Green Valley Apartments RWA', 'November Flat Rent Payment', 'NET_BANKING', 'UPI/20261102/RENT/458', 'COMPLETED'),
(3, 1, 1, 2, NULL, 'EXPENSE', 4850.50, 'INR', '2026-11-02 11:30:00', 'BigBasket Provisions', 'Monthly pantry and grocery refill', 'UPI', 'UPI/20261102/BB/9871', 'COMPLETED'),
(4, 1, 2, 4, NULL, 'EXPENSE', 2200.00, 'INR', '2026-11-02 14:45:00', 'Bharat Petroleum Station', 'Full petrol tank refill for car', 'CREDIT_CARD', 'POS/20261102/BPCL/12', 'COMPLETED'),
(5, 1, 4, 3, NULL, 'EXPENSE', 840.00, 'INR', '2026-11-02 18:20:00', 'Biryani Blues Restaurant', 'Weekend dinner with family', 'UPI', 'UPI/20261102/SWIGGY/99', 'COMPLETED')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- 6. Insert Monthly Budget & Category Allocations
INSERT INTO budgets (id, user_id, name, period_type, start_date, end_date, total_budget_amount, notification_threshold, is_active, version)
VALUES
(1, 1, 'November 2026 Living Budget', 'MONTHLY', '2026-11-01', '2026-11-30', 65000.00, 80.00, TRUE, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO budget_categories (id, budget_id, category_id, allocated_amount)
VALUES
(1, 1, 1, 22000.00), -- Housing & Rent
(2, 1, 2, 12000.00), -- Groceries
(3, 1, 3, 8000.00),  -- Dining Out
(4, 1, 4, 6000.00),  -- Transportation & Fuel
(5, 1, 5, 5000.00)   -- Utilities & Bills
ON DUPLICATE KEY UPDATE allocated_amount=VALUES(allocated_amount);

-- 7. Insert Savings Goals & Contributions
INSERT INTO savings_goals (id, user_id, title, description, target_amount, current_amount, target_date, status, priority, version)
VALUES
(1, 1, 'Emergency Fund (6 Months Expenses)', 'Liquid funds stored in sweep savings for contingency', 300000.00, 85000.00, '2027-06-30', 'IN_PROGRESS', 'HIGH', 0),
(2, 1, 'New Electric Two-Wheeler', 'Down payment for Ather 450X electric scooter', 120000.00, 35000.00, '2027-03-31', 'IN_PROGRESS', 'MEDIUM', 0)
ON DUPLICATE KEY UPDATE title=VALUES(title);

INSERT INTO goal_contributions (id, goal_id, account_id, amount, contribution_date, note)
VALUES
(1, 1, 1, 25000.00, '2026-11-01 10:00:00', 'Bonus allocation to emergency fund'),
(2, 2, 1, 10000.00, '2026-11-01 10:30:00', 'Monthly allocation for scooter')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- 8. Insert In-App Notifications
INSERT INTO notifications (id, user_id, title, message, type, is_read, action_url)
VALUES
(1, 1, 'Monthly Tech Salary Credited', 'Your salary of ₹1,15,000.00 was credited to HDFC Salary Savings.', 'SYSTEM_ALERT', TRUE, '/accounts'),
(2, 1, 'Budget Threshold Alert: Dining Out', 'You have exhausted 65% of your ₹8,000.00 Dining Out budget for November.', 'BUDGET_WARNING', FALSE, '/budgets'),
(3, 1, 'SIP Auto-Debit Reminder', 'Your Nifty 50 Mutual Fund SIP of ₹15,000.00 is scheduled for processing on 5th Nov.', 'RECURRING_DUE', FALSE, '/recurring')
ON DUPLICATE KEY UPDATE title=VALUES(title);
