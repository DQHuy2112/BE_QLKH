package com.example.auth_service.repository;

import com.example.auth_service.entity.AdRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdRoleRepository extends JpaRepository<AdRole, Long> {
    Optional<AdRole> findByRoleCode(String roleCode);
    
    @Query("SELECT r FROM AdRole r WHERE " +
           "(:search IS NULL OR LOWER(r.roleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.displayName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<AdRole> searchRoles(@Param("search") String search);
}
