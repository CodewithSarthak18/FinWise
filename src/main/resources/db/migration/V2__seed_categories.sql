-- =============================================================================
-- FinWise Seed Data Migration - V2__seed_categories.sql
-- Description: Inserts standard roles and default categories for Indian Personal Finance
-- =============================================================================

-- 1. Insert Core Roles
INSERT INTO roles (name, description) VALUES
('ROLE_USER', 'Standard authenticated FinWise personal finance user'),
('ROLE_ADMIN', 'System administrator with platform monitoring privileges')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 2. Insert System Default Expense Categories
INSERT INTO categories (user_id, parent_id, name, type, icon, color_code, is_system) VALUES
(NULL, NULL, 'Housing & Rent', 'EXPENSE', 'home', '#EF4444', TRUE),
(NULL, NULL, 'Groceries & Provisions', 'EXPENSE', 'shopping-cart', '#F59E0B', TRUE),
(NULL, NULL, 'Food & Dining Out', 'EXPENSE', 'utensils', '#F97316', TRUE),
(NULL, NULL, 'Transportation & Fuel', 'EXPENSE', 'car', '#3B82F6', TRUE),
(NULL, NULL, 'Utilities & Bills', 'EXPENSE', 'zap', '#8B5CF6', TRUE),
(NULL, NULL, 'Healthcare & Medical', 'EXPENSE', 'activity', '#EC4899', TRUE),
(NULL, NULL, 'Investments & SIPs', 'EXPENSE', 'trending-up', '#10B981', TRUE),
(NULL, NULL, 'Entertainment & Subscriptions', 'EXPENSE', 'film', '#6366F1', TRUE),
(NULL, NULL, 'Shopping & Personal Care', 'EXPENSE', 'shopping-bag', '#14B8A6', TRUE),
(NULL, NULL, 'Education & Books', 'EXPENSE', 'book-open', '#06B6D4', TRUE);

-- 3. Insert System Default Income Categories
INSERT INTO categories (user_id, parent_id, name, type, icon, color_code, is_system) VALUES
(NULL, NULL, 'Salary & Compensation', 'INCOME', 'briefcase', '#10B981', TRUE),
(NULL, NULL, 'Freelance & Consulting', 'INCOME', 'laptop', '#059669', TRUE),
(NULL, NULL, 'Dividends & Interest', 'INCOME', 'pie-chart', '#047857', TRUE),
(NULL, NULL, 'Rental Income', 'INCOME', 'building', '#065F46', TRUE),
(NULL, NULL, 'Cashback & Rewards', 'INCOME', 'gift', '#34D399', TRUE),
(NULL, NULL, 'Other Inflow', 'INCOME', 'plus-circle', '#6EE7B7', TRUE);
