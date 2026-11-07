-- =============================================================================
-- FinWise Migration - V5__seed_subscription_transactions.sql
-- Description: Insert sample historical expense transactions for User 1 (Rahul)
--              to populate the AI Subscription Detector with realistic candidates
-- =============================================================================

-- 1. Spotify India Monthly Debits (₹179/month across Aug, Sep, Oct 2026)
INSERT INTO transactions (id, user_id, account_id, category_id, recurring_transaction_id, transaction_type, amount, currency_code, transaction_date, merchant_name, description, payment_mode, reference_number, status)
VALUES
(101, 1, 1, 8, NULL, 'EXPENSE', 179.00, 'INR', '2026-08-15 10:30:00', 'Spotify India', 'Spotify Premium Family Plan', 'UPI', 'UPI/20260815/SPOTIFY/01', 'COMPLETED'),
(102, 1, 1, 8, NULL, 'EXPENSE', 179.00, 'INR', '2026-09-15 10:30:00', 'Spotify India', 'Spotify Premium Family Plan', 'UPI', 'UPI/20260915/SPOTIFY/02', 'COMPLETED'),
(103, 1, 1, 8, NULL, 'EXPENSE', 179.00, 'INR', '2026-10-15 10:30:00', 'Spotify India', 'Spotify Premium Family Plan', 'UPI', 'UPI/20261015/SPOTIFY/03', 'COMPLETED')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- 2. Cult.fit Gym Membership (₹1,499/month across Aug, Sep, Oct 2026)
INSERT INTO transactions (id, user_id, account_id, category_id, recurring_transaction_id, transaction_type, amount, currency_code, transaction_date, merchant_name, description, payment_mode, reference_number, status)
VALUES
(104, 1, 2, 6, NULL, 'EXPENSE', 1499.00, 'INR', '2026-08-05 09:15:00', 'Cult.fit Fitness Club', 'Cultpass Elite Gym Subscription', 'CREDIT_CARD', 'POS/20260805/CULT/88', 'COMPLETED'),
(105, 1, 2, 6, NULL, 'EXPENSE', 1499.00, 'INR', '2026-09-05 09:15:00', 'Cult.fit Fitness Club', 'Cultpass Elite Gym Subscription', 'CREDIT_CARD', 'POS/20260905/CULT/89', 'COMPLETED'),
(106, 1, 2, 6, NULL, 'EXPENSE', 1499.00, 'INR', '2026-10-05 09:15:00', 'Cult.fit Fitness Club', 'Cultpass Elite Gym Subscription', 'CREDIT_CARD', 'POS/20261005/CULT/90', 'COMPLETED')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- 3. Amazon Prime Video Subscription (₹299/month across Aug, Sep, Oct 2026)
INSERT INTO transactions (id, user_id, account_id, category_id, recurring_transaction_id, transaction_type, amount, currency_code, transaction_date, merchant_name, description, payment_mode, reference_number, status)
VALUES
(107, 1, 1, 8, NULL, 'EXPENSE', 299.00, 'INR', '2026-08-28 14:20:00', 'Amazon Prime', 'Prime Video & Delivery Plan', 'UPI', 'UPI/20260828/AMZN/11', 'COMPLETED'),
(108, 1, 1, 8, NULL, 'EXPENSE', 299.00, 'INR', '2026-09-28 14:20:00', 'Amazon Prime', 'Prime Video & Delivery Plan', 'UPI', 'UPI/20260928/AMZN/12', 'COMPLETED'),
(109, 1, 1, 8, NULL, 'EXPENSE', 299.00, 'INR', '2026-10-28 14:20:00', 'Amazon Prime', 'Prime Video & Delivery Plan', 'UPI', 'UPI/20261028/AMZN/13', 'COMPLETED')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);
