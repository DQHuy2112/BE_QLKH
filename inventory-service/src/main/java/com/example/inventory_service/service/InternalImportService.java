package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;

import java.time.LocalDate;
import java.util.List;

public interface InternalImportService {

    SupplierImportDto create(SupplierImportRequest request);

    List<SupplierImportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to);

    SupplierImportDto getById(Long id);

    SupplierImportDto update(Long id, SupplierImportRequest request);

    // Xác nhận nhập kho (PENDING → IMPORTED)
    SupplierImportDto confirm(Long id);

    // Hủy phiếu nhập (PENDING → CANCELLED)
    SupplierImportDto cancel(Long id);
}
