package com.example.inventory_service.service;

import java.time.LocalDate;
import java.util.List;

import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;

public interface SupplierImportService {

    SupplierImportDto create(SupplierImportRequest request);

    List<SupplierImportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to);

    SupplierImportDto getById(Long id);

    SupplierImportDto update(Long id, SupplierImportRequest request);
}
