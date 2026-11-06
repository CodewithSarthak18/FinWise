-- =============================================================================
-- FinWise Migration - V4__fix_demo_user_passwords.sql
-- Description: Update demo users with valid BCrypt hash for DevPassword#2026
--              and clean up test email so the user can register smoothly.
-- =============================================================================

-- 1. Update demo users (Rahul and Ananya) with valid BCrypt hash (cost 12) for 'DevPassword#2026'
UPDATE users 
SET password_hash = '$2a$12$11sGCdNeF2AfPEW2zcNbCu8YbkYFBWW28BZQLwm7VvnmldGmGPwI2'
WHERE email IN ('rahul.verma@example.com', 'ananya.sen@example.com');

-- 2. Delete test user 'sarthakdabhade017@gmail.com' so the user can register cleanly
DELETE FROM refresh_tokens 
WHERE user_id IN (SELECT id FROM users WHERE email = 'sarthakdabhade017@gmail.com');

DELETE FROM user_roles 
WHERE user_id IN (SELECT id FROM users WHERE email = 'sarthakdabhade017@gmail.com');

DELETE FROM users 
WHERE email = 'sarthakdabhade017@gmail.com';
