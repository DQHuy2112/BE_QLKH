package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "token_blacklist")
@Data
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "expiry_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;

    @Column(name = "blacklisted_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date blacklistedAt;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;
}


