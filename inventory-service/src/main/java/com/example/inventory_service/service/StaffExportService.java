package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Service xử lý phiếu xuất cho nhân viên (STAFF)
 */
public interface StaffExportService {

    SupplierExportDto create(SupplierExportRequest request);

    List<SupplierExportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to);

    SupplierExportDto getById(Long id);

    SupplierExportDto update(Long id, SupplierExportRequest request);

    // Xác nhận xuất kho (PENDING → EXPORTED)
    SupplierExportDto confirm(Long id);

    // Hủy phiếu xuất (PENDING → CANCELLED)
    SupplierExportDto cancel(Long id);
}
