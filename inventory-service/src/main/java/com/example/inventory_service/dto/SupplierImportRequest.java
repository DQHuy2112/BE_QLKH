package com.example.inventory_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class SupplierImportRequest {

    private String code; // optional: nếu không gửi, BE sẽ tự sinh

    private Long storeId; // Kho đích (kho nhận hàng)
    private Long supplierId; // ID nhà cung cấp (cho phiếu nhập từ NCC)
    private Long sourceStoreId; // ID kho nguồn (cho phiếu nhập nội bộ)

    private String note;
    private String description;

    private List<String> attachmentImages; // đường dẫn ảnh FE gửi (/uploads/... hoặc full URL)

    private List<ImportDetailRequest> items;
}
