package com.example.inventory_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "shop_export_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopExportDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "export_details_id")
    private Long id;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    /**
     * Tạm thời có thể = null, sau này bạn mapping thật với phiếu nhập (FIFO,...)
     */
    @Column(name = "import_details_id", nullable = true)
    private Long importDetailsId;

    @Column(name = "products_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exports_id")
    private ShopExport export;
}
