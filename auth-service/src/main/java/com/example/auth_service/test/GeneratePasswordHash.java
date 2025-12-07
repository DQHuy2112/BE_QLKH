package com.example.auth_service.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class để generate BCrypt password hash cho test users
 * 
 * Cách sử dụng:
 * 1. Chạy main method
 * 2. Nhập password cần hash
 * 3. Copy hash được generate vào script SQL
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Password mặc định cho test: "password123"
        String password = "password123";
        
        // Generate hash
        String hash = encoder.encode(password);
        
        System.out.println("============================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("============================================");
        System.out.println("\nCopy hash trên vào script SQL để tạo users test.");
        
        // Verify hash
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verify: " + (matches ? "✓ OK" : "✗ FAILED"));
    }
}

