package com.example.inventory_service.service;

import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;

import java.time.LocalDate;
import java.util.List;

public interface ExportService {

    SupplierExportDto create(SupplierExportRequest req);

    List<SupplierExportDto> search(String status, String code, LocalDate from, LocalDate to);

    SupplierExportDto getById(Long id);

    SupplierExportDto update(Long id, SupplierExportRequest req);

    SupplierExportDto confirm(Long id);

    SupplierExportDto cancel(Long id);

    List<SupplierExportDto> getAll();

    List<SupplierExportDto> getByStore(Long storeId);

    List<SupplierExportDto> getByOrder(Long orderId);
}
