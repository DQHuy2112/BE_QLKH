package com.example.auth_service.service.impl;

import com.example.auth_service.entity.AdUser;
import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.NotFoundException;
import com.example.auth_service.repository.AdUserRepository;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AdUserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, AdUserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        AdUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(generateOpaqueToken());
        Date now = new Date();
        rt.setCreatedAt(now);
        rt.setExpiryDate(new Date(now.getTime() + refreshExpirationMs));
        rt.setRevoked(false);

        return refreshTokenRepository.save(rt);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ"));

        if (Boolean.TRUE.equals(rt.getRevoked())) {
            throw new BadRequestException("Refresh token đã bị thu hồi");
        }

        if (rt.getExpiryDate() == null || rt.getExpiryDate().before(new Date())) {
            throw new BadRequestException("Refresh token đã hết hạn");
        }

        return rt;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUser_Id(userId);
        for (RefreshToken rt : tokens) {
            if (!Boolean.TRUE.equals(rt.getRevoked())) {
                rt.setRevoked(true);
            }
        }
        refreshTokenRepository.saveAll(tokens);
    }

    @Scheduled(fixedRateString = "${app.auth.cleanup-rate-ms:3600000}")
    @Transactional
    public void cleanupExpiredTokens() {
        long deleted = refreshTokenRepository.deleteByExpiryDateBefore(new Date());
        if (deleted > 0) {
            log.info("[AUTH] Cleanup refresh tokens deleted={}", deleted);
        }
    }

    private String generateOpaqueToken() {
        // 32 bytes -> 43 chars base64url
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}


