package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;

import java.time.LocalDate;
import java.util.List;

public interface ImportService {

    SupplierImportDto create(SupplierImportRequest req);

    List<SupplierImportDto> search(String status, String code, LocalDate from, LocalDate to);

    SupplierImportDto getById(Long id);

    SupplierImportDto update(Long id, SupplierImportRequest req);

    SupplierImportDto confirm(Long id);

    SupplierImportDto cancel(Long id);

    List<SupplierImportDto> getAll();

    List<SupplierImportDto> getByStore(Long storeId);
}
