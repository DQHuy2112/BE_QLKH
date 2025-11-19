package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;

import java.time.LocalDate;
import java.util.List;

public interface SupplierExportService {

    SupplierExportDto create(SupplierExportRequest request);

    List<SupplierExportDto> search(
            String status,
            String code,
            LocalDate fromDate,
            LocalDate toDate);

    SupplierExportDto getById(Long id);

    SupplierExportDto update(Long id, SupplierExportRequest request);
}
