package com.example.auth_service.service;

public interface TokenBlacklistService {
    void blacklistToken(String token, Long userId, String reason);
    boolean isTokenBlacklisted(String token);
}


