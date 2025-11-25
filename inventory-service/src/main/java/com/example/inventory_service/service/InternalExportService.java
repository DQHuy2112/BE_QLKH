package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Service xử lý phiếu xuất nội bộ (INTERNAL)
 * Xuất hàng từ kho này sang kho khác
 */
public interface InternalExportService {
    SupplierExportDto create(SupplierExportRequest request);

    List<SupplierExportDto> search(String status, String code, LocalDate from, LocalDate to);

    SupplierExportDto getById(Long id);

    SupplierExportDto update(Long id, SupplierExportRequest request);

    SupplierExportDto confirm(Long id);

    SupplierExportDto cancel(Long id);
}
