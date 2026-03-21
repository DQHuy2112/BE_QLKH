-- Advanced Auth Features schema changes (MySQL)
-- Run manually because spring.jpa.hibernate.ddl-auto is set to none

-- 1) Extend ad_users for email verification & password reset
ALTER TABLE ad_users
  ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN email_verification_token VARCHAR(255) NULL,
  ADD COLUMN email_verification_token_expiry DATETIME NULL,
  ADD COLUMN password_reset_token VARCHAR(255) NULL,
  ADD COLUMN password_reset_token_expiry DATETIME NULL;

-- 2) Refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token VARCHAR(512) NOT NULL,
  user_id BIGINT NOT NULL,
  expiry_date DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_tokens_token (token),
  KEY idx_refresh_tokens_user (user_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES ad_users(user_id) ON DELETE CASCADE
);

-- 3) Token blacklist table (store invalidated access tokens)
CREATE TABLE IF NOT EXISTS token_blacklist (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token VARCHAR(512) NOT NULL,
  user_id BIGINT NULL,
  expiry_date DATETIME NOT NULL,
  blacklisted_at DATETIME NOT NULL,
  reason VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_token_blacklist_token (token),
  KEY idx_token_blacklist_expiry (expiry_date),
  KEY idx_token_blacklist_user (user_id)
);


