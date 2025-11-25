package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;

import java.time.LocalDate;
import java.util.List;

public interface ExportOrderService {

    SupplierExportDto create(SupplierExportRequest request);

    List<SupplierExportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to);

    SupplierExportDto getById(Long id);

    SupplierExportDto update(Long id, SupplierExportRequest request);

    // Duyệt lệnh xuất (PENDING → APPROVED)
    SupplierExportDto approve(Long id);

    // Hủy lệnh xuất (PENDING → CANCELLED)
    SupplierExportDto cancel(Long id);
}
