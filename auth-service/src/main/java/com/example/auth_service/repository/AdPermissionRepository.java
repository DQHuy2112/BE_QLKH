package com.example.auth_service.repository;

import com.example.auth_service.entity.AdPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdPermissionRepository extends JpaRepository<AdPermission, Long> {
    Optional<AdPermission> findByPermissionCode(String permissionCode);
    
    @Query("SELECT p FROM AdPermission p WHERE " +
           "(:search IS NULL OR LOWER(p.permissionCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<AdPermission> searchPermissions(@Param("search") String search);
}

