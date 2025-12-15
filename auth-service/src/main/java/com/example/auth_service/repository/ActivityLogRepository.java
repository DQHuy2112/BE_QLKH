package com.example.auth_service.repository;

import com.example.auth_service.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByUserId(Long userId, Pageable pageable);
    
    Page<ActivityLog> findByAction(String action, Pageable pageable);
    
    @Query("SELECT a FROM ActivityLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) AND " +
           "(:ipAddress IS NULL OR a.ipAddress LIKE CONCAT('%', :ipAddress, '%')) AND " +
           "(:userAgent IS NULL OR a.userAgent LIKE CONCAT('%', :userAgent, '%'))")
    Page<ActivityLog> searchActivityLogs(
        @Param("userId") Long userId,
        @Param("action") String action,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("ipAddress") String ipAddress,
        @Param("userAgent") String userAgent,
        Pageable pageable
    );
}

