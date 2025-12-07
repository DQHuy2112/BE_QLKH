package com.example.inventory_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "shop_imports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imports_id")
    private Long id;

    @Column(name = "import_code")
    private String code;

    @Column(name = "import_type")
    private String importType; // "SUPPLIER"

    @Column(name = "status")
    private String status;

    @Column(name = "imports_date")
    private Date importsDate;

    @Column(name = "stores_id")
    private Long storeId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "note")
    private String note;

    @Column(name = "description")
    private String description;

    @Column(name = "attachment_image")
    private String attachmentImage; // /uploads/...

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    // Audit fields
    @Column(name = "created_by")
    private Long createdBy; // userId của người tạo

    @Column(name = "approved_by")
    private Long approvedBy; // userId của người duyệt

    @Column(name = "approved_at")
    private Date approvedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy; // userId của người từ chối

    @Column(name = "rejected_at")
    private Date rejectedAt;

    @Column(name = "imported_by")
    private Long importedBy; // userId của người nhập kho

    @Column(name = "imported_at")
    private Date importedAt;
}
