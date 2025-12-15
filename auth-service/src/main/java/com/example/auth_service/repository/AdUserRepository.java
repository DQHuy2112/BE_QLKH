package com.example.auth_service.repository;

import com.example.auth_service.entity.AdUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdUserRepository extends JpaRepository<AdUser, Long> {
    Optional<AdUser> findByUsername(String username);
    
    @Query("SELECT DISTINCT u FROM AdUser u LEFT JOIN u.roles r WHERE " +
           "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:phone IS NULL OR u.phone LIKE CONCAT('%', :phone, '%')) AND " +
           "(:active IS NULL OR u.active = :active) AND " +
           "(:roleId IS NULL OR r.id = :roleId)")
    Page<AdUser> searchUsers(
        @Param("username") String username,
        @Param("email") String email,
        @Param("phone") String phone,
        @Param("active") Boolean active,
        @Param("roleId") Long roleId,
        Pageable pageable
    );
}