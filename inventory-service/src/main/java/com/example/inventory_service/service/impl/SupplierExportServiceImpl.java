package com.example.inventory_service.service.impl;

import com.example.inventory_service.dto.ExportDetailRequest;
import com.example.inventory_service.dto.SupplierExportDto;
import com.example.inventory_service.dto.SupplierExportRequest;
import com.example.inventory_service.entity.ShopExport;
import com.example.inventory_service.entity.ShopExportDetail;
import com.example.inventory_service.exception.NotFoundException;
import com.example.inventory_service.repository.ShopExportDetailRepository;
import com.example.inventory_service.repository.ShopExportRepository;
import com.example.inventory_service.service.SupplierExportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierExportServiceImpl implements SupplierExportService {

    private final ShopExportRepository exportRepo;
    private final ShopExportDetailRepository detailRepo;

    public SupplierExportServiceImpl(ShopExportRepository exportRepo,
                                     ShopExportDetailRepository detailRepo) {
        this.exportRepo = exportRepo;
        this.detailRepo = detailRepo;
    }

    @Override
    @Transactional
    public SupplierExportDto create(SupplierExportRequest req) {
        java.util.Date now = new java.util.Date();

        ShopExport export = new ShopExport();
        export.setCode(req.getCode() != null ? req.getCode() : generateCode());
        export.setExportType("SUPPLIER");
        export.setStoreId(req.getStoreId());
        export.setSupplierId(req.getSupplierId());
        export.setNote(req.getNote());
        export.setDescription(req.getDescription());
        export.setStatus("PENDING");
        export.setExportsDate(now);
        // TODO: set userId từ JWT nếu cần
        export.setCreatedAt(now);
        export.setUpdatedAt(now);

        export = exportRepo.save(export);

        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        for (ExportDetailRequest item : req.getItems()) {
            ShopExportDetail d = new ShopExportDetail();
            d.setExport(export);
            d.setProductId(item.getProductId());
            d.setImportDetailsId(item.getImportDetailsId());
            d.setQuantity(item.getQuantity());
            d.setUnitPrice(item.getUnitPrice());

            BigDecimal line = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(line);

            details.add(d);
        }

        detailRepo.saveAll(details);

        return toDto(export, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierExportDto> search(String status,
                                          String code,
                                          LocalDate from,
                                          LocalDate to) {

        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        List<ShopExport> list = exportRepo.searchSupplierExports(
                status,
                code,
                fromDate,
                toDate
        );

        List<SupplierExportDto> result = new ArrayList<>();
        for (ShopExport e : list) {
            result.add(toDtoWithCalcTotal(e));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierExportDto getById(Long id) {
        ShopExport e = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));
        return toDtoWithCalcTotal(e);
    }

    // ========= helpers ========= //

    private String generateCode() {
        // TODO: sinh mã chuẩn (sequence), tạm dùng timestamp
        return "PXNCC" + System.currentTimeMillis();
    }

    private SupplierExportDto toDtoWithCalcTotal(ShopExport e) {
        BigDecimal total = BigDecimal.ZERO;
        if (e.getDetails() != null) {
            for (ShopExportDetail d : e.getDetails()) {
                BigDecimal line = d.getUnitPrice()
                        .multiply(BigDecimal.valueOf(d.getQuantity()));
                total = total.add(line);
            }
        }
        return toDto(e, total);
    }

    private SupplierExportDto toDto(ShopExport e, BigDecimal total) {
        SupplierExportDto dto = new SupplierExportDto();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setStoreId(e.getStoreId());
        dto.setSupplierId(e.getSupplierId());
        dto.setStatus(e.getStatus());
        dto.setExportsDate(e.getExportsDate());
        dto.setNote(e.getNote());
        dto.setTotalValue(total);

        // FE sẽ tự join tên NCC từ product-service
        dto.setSupplierName(null);
        return dto;
    }
}
