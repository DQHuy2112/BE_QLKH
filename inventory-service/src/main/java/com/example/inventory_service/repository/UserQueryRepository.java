package com.example.inventory_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("userQueryRepository")
public class UserQueryRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public UserQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<Long> findUserIdByUsername(String username) {
        try {
            String sql = "SELECT user_id FROM ad_users WHERE username = ? LIMIT 1";
            Long userId = jdbcTemplate.queryForObject(sql, Long.class, username);
            return Optional.ofNullable(userId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    public Optional<String> findFullNameByUsername(String username) {
        try {
            String sql = "SELECT TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, ''))) FROM ad_users WHERE username = ? LIMIT 1";
            String fullName = jdbcTemplate.queryForObject(sql, String.class, username);
            if (fullName != null && !fullName.trim().isEmpty()) {
                return Optional.of(fullName.trim());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    public Optional<String> findFullNameByUserId(Long userId) {
        try {
            String sql = "SELECT TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, ''))) FROM ad_users WHERE user_id = ? LIMIT 1";
            String fullName = jdbcTemplate.queryForObject(sql, String.class, userId);
            if (fullName != null && !fullName.trim().isEmpty()) {
                return Optional.of(fullName.trim());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    public Optional<String> findUsernameByUserId(Long userId) {
        try {
            String sql = "SELECT username FROM ad_users WHERE user_id = ? LIMIT 1";
            String username = jdbcTemplate.queryForObject(sql, String.class, userId);
            return Optional.ofNullable(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Lấy role của user từ userId
     * Tìm role từ bảng ad_user_has_roles và ad_roles
     */
    public Optional<String> findRoleByUserId(Long userId) {
        try {
            // Lấy role_code từ bảng ad_user_has_roles và ad_roles
            String sql = "SELECT r.role_code FROM ad_user_has_roles uhr " +
                        "INNER JOIN ad_roles r ON uhr.roles_id = r.roles_id " +
                        "WHERE uhr.user_id = ? " +
                        "ORDER BY r.roles_id ASC LIMIT 1";
            try {
                String role = jdbcTemplate.queryForObject(sql, String.class, userId);
                if (role != null && !role.trim().isEmpty()) {
                    return Optional.of(role.trim());
                }
            } catch (Exception e) {
                // Log lỗi để debug
                System.err.println("⚠️ Failed to get role from ad_user_has_roles for userId " + userId + ": " + e.getMessage());
            }
            
            // Thử lấy display_name nếu không có role_code
            try {
                String sql2 = "SELECT r.display_name FROM ad_user_has_roles uhr " +
                             "INNER JOIN ad_roles r ON uhr.roles_id = r.roles_id " +
                             "WHERE uhr.user_id = ? " +
                             "ORDER BY r.roles_id ASC LIMIT 1";
                String role = jdbcTemplate.queryForObject(sql2, String.class, userId);
                if (role != null && !role.trim().isEmpty()) {
                    return Optional.of(role.trim());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Failed to get display_name from ad_roles for userId " + userId + ": " + e.getMessage());
            }
            
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("⚠️ Exception in findRoleByUserId for userId " + userId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}

