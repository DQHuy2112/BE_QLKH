package com.example.auth_service.repository;

import com.example.auth_service.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {
    boolean existsByToken(String token);
    long deleteByExpiryDateBefore(Date cutoff);
}


