package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "ad_permissions")
@Data
public class AdPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permissions_id")
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String permissionCode;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}

