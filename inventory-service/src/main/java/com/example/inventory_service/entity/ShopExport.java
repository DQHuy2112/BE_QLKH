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
    private String code; // mã phiếu

    @Column(name = "export_type")
    private String exportType; // SUPPLIER / INTERNAL / SALE_EMPLOYEE / ORDER

    @Column(name = "note")
    private String note;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status; // PENDING / APPROVED / ...

    @Column(name = "exports_date")
    private Date exportsDate;

    @Column(name = "stores_id")
    private Long storeId;

    @Column(name = "supplier_id")
    private Long supplierId; // chỉ dùng khi exportType = SUPPLIER

    @Column(name = "user_id")
    private Long userId; // người lập phiếu

    @Column(name = "order_id")
    private Long orderId; // nếu xuất theo đơn

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @OneToMany(mappedBy = "export", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopExportDetail> details;
}
