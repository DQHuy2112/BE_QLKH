package com.example.auth_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @PostConstruct
    public void printSecretInfo() {
        log.info("[AUTH] jwt.secret length = {}", secret != null ? secret.length() : null);
    }

    /* ====== KEY ====== */
    private Key getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* ====== GENERATE TOKEN ====== */

    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // nhúng list role vào token cho gateway dùng nếu cần
        claims.put("roles",
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
        return buildToken(claims, userDetails.getUsername(), expirationMs);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("roles",
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
        return buildToken(claims, userDetails.getUsername(), expirationMs);
    }

    public String generateRefreshJwt(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails.getUsername(), refreshExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    

    /* ====== EXTRACT / VALIDATE (giữ cả tên cũ cho đỡ lỗi) ====== */

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Vẫn có thể extract username từ expired token
            return e.getClaims().getSubject();
        }
    }

    // tên cũ thường gặp
    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails);
    }

    private boolean isTokenExpired(String token) {
        try {
            Date exp = extractClaim(token, Claims::getExpiration);
            return exp != null && exp.before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token đã expired
            return true;
        } catch (Exception e) {
            // Token không hợp lệ
            return true;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Date extractExpirationAllowExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
            return Collections.emptyList();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            Object rolesObj = e.getClaims().get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
