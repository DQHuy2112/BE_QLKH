package com.example.auth_service.service.impl;

import com.example.auth_service.entity.TokenBlacklist;
import com.example.auth_service.repository.TokenBlacklistRepository;
import com.example.auth_service.security.JwtService;
import com.example.auth_service.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    public TokenBlacklistServiceImpl(TokenBlacklistRepository tokenBlacklistRepository, JwtService jwtService) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public void blacklistToken(String token, Long userId, String reason) {
        if (token == null || token.isBlank()) return;
        if (tokenBlacklistRepository.existsByToken(token)) return;

        Date exp = jwtService.extractExpirationAllowExpired(token);
        if (exp == null) {
            // fallback: 30 minutes
            exp = new Date(System.currentTimeMillis() + 30L * 60L * 1000L);
        }

        TokenBlacklist bl = new TokenBlacklist();
        bl.setToken(token);
        bl.setUserId(userId);
        bl.setReason(reason != null ? reason : "UNKNOWN");
        bl.setBlacklistedAt(new Date());
        bl.setExpiryDate(exp);

        tokenBlacklistRepository.save(bl);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        return tokenBlacklistRepository.existsByToken(token);
    }

    @Scheduled(fixedRateString = "${app.auth.cleanup-rate-ms:3600000}")
    @Transactional
    public void cleanupExpiredTokens() {
        long deleted = tokenBlacklistRepository.deleteByExpiryDateBefore(new Date());
        if (deleted > 0) {
            log.info("[AUTH] Cleanup token blacklist deleted={}", deleted);
        }
    }
}


