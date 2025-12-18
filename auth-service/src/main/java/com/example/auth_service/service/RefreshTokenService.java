package com.example.auth_service.service;

import com.example.auth_service.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    RefreshToken validateRefreshToken(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserTokens(Long userId);
}


