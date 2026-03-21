-- ============================================
-- Script SQL để tạo Roles và Users Test
-- ============================================

-- 1. Tạo các Roles
-- ============================================
INSERT INTO `ad_roles` (`role_code`, `display_name`, `created_at`, `updated_at`) VALUES
('ADMIN', 'Quản trị viên', NOW(), NOW()),
('MANAGER', 'Quản lý', NOW(), NOW()),
('STAFF', 'Nhân viên', NOW(), NOW()),
('USER', 'Người dùng', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    `display_name` = VALUES(`display_name`),
    `updated_at` = NOW();

-- 2. Tạo các Users Test
-- ============================================
-- Password mặc định cho tất cả user: "password123"
-- BCrypt hash của "password123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- (Bạn có thể generate hash mới bằng BCryptPasswordEncoder)

-- User ADMIN
INSERT INTO `ad_users` (
    `username`, 
    `password`, 
    `first_name`, 
    `last_name`, 
    `email`, 
    `phone`, 
    `active`, 
    `created_at`, 
    `updated_at`
) VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
    'Admin',
    'System',
    'admin@example.com',
    '0123456789',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    `password` = VALUES(`password`),
    `updated_at` = NOW();

-- User MANAGER
INSERT INTO `ad_users` (
    `username`, 
    `password`, 
    `first_name`, 
    `last_name`, 
    `email`, 
    `phone`, 
    `active`, 
    `created_at`, 
    `updated_at`
) VALUES (
    'manager',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
    'Manager',
    'Test',
    'manager@example.com',
    '0123456788',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    `password` = VALUES(`password`),
    `updated_at` = NOW();

-- User STAFF
INSERT INTO `ad_users` (
    `username`, 
    `password`, 
    `first_name`, 
    `last_name`, 
    `email`, 
    `phone`, 
    `active`, 
    `created_at`, 
    `updated_at`
) VALUES (
    'staff',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
    'Staff',
    'Test',
    'staff@example.com',
    '0123456787',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    `password` = VALUES(`password`),
    `updated_at` = NOW();

-- User USER (chỉ xem)
INSERT INTO `ad_users` (
    `username`, 
    `password`, 
    `first_name`, 
    `last_name`, 
    `email`, 
    `phone`, 
    `active`, 
    `created_at`, 
    `updated_at`
) VALUES (
    'user',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
    'User',
    'Test',
    'user@example.com',
    '0123456786',
    1,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    `password` = VALUES(`password`),
    `updated_at` = NOW();

-- 3. Gán Roles cho Users
-- ============================================

-- Xóa các gán role cũ (nếu có)
DELETE FROM `ad_user_has_roles` 
WHERE `user_id` IN (
    SELECT `user_id` FROM `ad_users` WHERE `username` IN ('admin', 'manager', 'staff', 'user')
);

-- Gán role ADMIN cho user 'admin'
INSERT INTO `ad_user_has_roles` (`user_id`, `roles_id`)
SELECT u.`user_id`, r.`roles_id`
FROM `ad_users` u, `ad_roles` r
WHERE u.`username` = 'admin' AND r.`role_code` = 'ADMIN';

-- Gán role MANAGER cho user 'manager'
INSERT INTO `ad_user_has_roles` (`user_id`, `roles_id`)
SELECT u.`user_id`, r.`roles_id`
FROM `ad_users` u, `ad_roles` r
WHERE u.`username` = 'manager' AND r.`role_code` = 'MANAGER';

-- Gán role STAFF cho user 'staff'
INSERT INTO `ad_user_has_roles` (`user_id`, `roles_id`)
SELECT u.`user_id`, r.`roles_id`
FROM `ad_users` u, `ad_roles` r
WHERE u.`username` = 'staff' AND r.`role_code` = 'STAFF';

-- Gán role USER cho user 'user'
INSERT INTO `ad_user_has_roles` (`user_id`, `roles_id`)
SELECT u.`user_id`, r.`roles_id`
FROM `ad_users` u, `ad_roles` r
WHERE u.`username` = 'user' AND r.`role_code` = 'USER';

-- 4. Kiểm tra kết quả
-- ============================================
-- Xem danh sách users và roles của họ
SELECT 
    u.`username`,
    u.`email`,
    u.`active`,
    GROUP_CONCAT(r.`role_code` ORDER BY r.`role_code` SEPARATOR ', ') AS roles
FROM `ad_users` u
LEFT JOIN `ad_user_has_roles` uhr ON u.`user_id` = uhr.`user_id`
LEFT JOIN `ad_roles` r ON uhr.`roles_id` = r.`roles_id`
WHERE u.`username` IN ('admin', 'manager', 'staff', 'user')
GROUP BY u.`user_id`, u.`username`, u.`email`, u.`active`
ORDER BY u.`username`;

-- ============================================
-- THÔNG TIN ĐĂNG NHẬP TEST
-- ============================================
-- Username: admin     | Password: password123 | Role: ADMIN   (Tất cả quyền)
-- Username: manager  | Password: password123 | Role: MANAGER (Duyệt/Từ chối)
-- Username: staff    | Password: password123 | Role: STAFF   (Tạo/Xem)
-- Username: user     | Password: password123 | Role: USER    (Chỉ xem)
-- ============================================

