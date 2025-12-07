package com.example.inventory_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "shop_exports")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ShopExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exports_id")
    private Long id;

    @Column(name = "export_code")
    private String code; // Mã phiếu (VD: PXNCC202511...)

    /**
     * Luôn = 'ORDER' (xuất cho khách hàng)
     */
    @Column(name = "export_type", nullable = false)
    private String exportType = "ORDER";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "description")
    private String description;

    /**
     * PENDING / APPROVED / REJECTED / EXPORTED / RETURNED...
     */
    @Column(name = "status")
    private String status;

    @Column(name = "exports_date")
    private Date exportsDate;

    @Column(name = "stores_id")
    private Long storeId;

    /**
     * Người lập phiếu
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Nếu xuất theo đơn hàng
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * Khách hàng - BẮT BUỘC (NOT NULL)
     * Phiếu xuất chỉ dùng cho khách hàng
     */
    @Column(name = "customers_id", nullable = false)
    private Long customerId;

    /**
     * Thông tin khách hàng (nếu nhập trực tiếp không qua shop_customers)
     */
    @Column(name = "customer_name", columnDefinition = "VARCHAR(255)")
    private String customerName;

    @Column(name = "customer_phone", columnDefinition = "VARCHAR(50)")
    private String customerPhone;

    @Column(name = "customer_address", columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "attachment_image")
    private String attachmentImage; // /uploads/...

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

    @Column(name = "exported_by")
    private Long exportedBy; // userId của người xuất kho

    @Column(name = "exported_at")
    private Date exportedAt;
}
