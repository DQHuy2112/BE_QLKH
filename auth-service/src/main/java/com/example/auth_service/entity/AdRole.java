package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ad_roles")
@Data
public class AdRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roles_id")   
    private Long id;             

    @Column(name = "role_code", unique = true, nullable = false)
    private String roleCode;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ad_role_has_permissions",
        joinColumns = @JoinColumn(name = "roles_id"),
        inverseJoinColumns = @JoinColumn(name = "permissions_id")
    )
    private Set<AdPermission> permissions = new HashSet<>();
}