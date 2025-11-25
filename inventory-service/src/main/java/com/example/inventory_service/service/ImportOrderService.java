package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;

import java.time.LocalDate;
import java.util.List;

public interface ImportOrderService {

    SupplierImportDto create(SupplierImportRequest request);

    List<SupplierImportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to);

    SupplierImportDto getById(Long id);

    SupplierImportDto update(Long id, SupplierImportRequest request);

    // Duyệt lệnh nhập (PENDING → APPROVED)
    SupplierImportDto approve(Long id);

    // Hủy lệnh nhập (PENDING → CANCELLED)
    SupplierImportDto cancel(Long id);
}
