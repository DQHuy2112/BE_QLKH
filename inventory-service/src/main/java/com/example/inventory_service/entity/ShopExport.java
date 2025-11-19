package com.example.inventory_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

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
     * SUPPLIER : Xuất/nhập với nhà cung cấp
     * INTERNAL : Xuất/nhập nội bộ
     * SALES : Xuất/nhập với NVBH / đơn hàng
     */
    @Column(name = "export_type")
    private String exportType;

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
     * Chỉ dùng khi exportType = SUPPLIER
     */
    @Column(name = "supplier_id")
    private Long supplierId;

    /**
     * Người lập phiếu (dùng cho NVBH hoặc audit sau này)
     * Có thể NULL cho phiếu SUPPLIER / INTERNAL
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Nếu xuất theo đơn hàng (NVBH)
     * NULL cho SUPPLIER / INTERNAL
     */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @OneToMany(mappedBy = "export", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopExportDetail> details;
}
