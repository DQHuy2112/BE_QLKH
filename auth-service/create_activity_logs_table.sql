-- Create activity_logs table
CREATE TABLE IF NOT EXISTS `activity_logs` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `username` VARCHAR(191) NOT NULL,
  `action` VARCHAR(100) NOT NULL COMMENT 'LOGIN, CREATE_USER, UPDATE_USER, DELETE_USER, etc.',
  `resource_type` VARCHAR(100) DEFAULT NULL COMMENT 'USER, ROLE, PERMISSION, etc.',
  `resource_id` BIGINT DEFAULT NULL,
  `resource_name` VARCHAR(255) DEFAULT NULL,
  `details` TEXT DEFAULT NULL COMMENT 'JSON string with additional details',
  `ip_address` VARCHAR(45) DEFAULT NULL,
  `user_agent` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action` (`action`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

