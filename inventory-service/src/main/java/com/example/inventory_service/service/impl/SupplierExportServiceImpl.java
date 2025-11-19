package com.example.inventory_service.service.impl;

import com.example.inventory_service.dto.ExportDetailDto;
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

        // ===== 1. Tạo phiếu xuất NCC (header) =====
        ShopExport export = new ShopExport();
        // Nếu FE không gửi code -> tự sinh
        export.setCode(req.getCode() != null ? req.getCode() : generateCode());
        export.setExportType("SUPPLIER");
        export.setStoreId(req.getStoreId());
        export.setSupplierId(req.getSupplierId());
        export.setNote(req.getNote());
        export.setDescription(req.getDescription());
        export.setStatus("PENDING");
        export.setExportsDate(now);

        // Cho phép NULL, sau này khi làm NVBH sẽ set user theo JWT
        export.setUserId(null);
        export.setOrderId(null);

        export.setCreatedAt(now);
        export.setUpdatedAt(now);

        export = exportRepo.save(export);

        // ===== 2. Chi tiết phiếu xuất =====
        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        for (ExportDetailRequest item : req.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                continue; // bỏ qua dòng không hợp lệ
            }
            if (item.getUnitPrice() == null) {
                continue;
            }

            ShopExportDetail d = new ShopExportDetail();
            d.setExport(export);
            d.setProductId(item.getProductId());
            d.setImportDetailsId(item.getImportDetailsId()); // hiện tại có thể là 0
            d.setQuantity(item.getQuantity());
            d.setUnitPrice(item.getUnitPrice());

            BigDecimal line = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(line);

            details.add(d);
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        // Nếu muốn, có thể set vào entity để khi toDtoWithCalcTotal không bị lazy
        export.setDetails(details);

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
                toDate);

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

    @Override
    @Transactional
    public SupplierExportDto update(Long id, SupplierExportRequest req) {
        // Tìm phiếu xuất cũ
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        // Cập nhật thông tin phiếu xuất
        if (req.getCode() != null && !req.getCode().isBlank()) {
            export.setCode(req.getCode());
        }
        export.setStoreId(req.getStoreId());
        export.setSupplierId(req.getSupplierId());
        export.setNote(req.getNote());
        export.setDescription(req.getDescription());
        export.setUpdatedAt(new java.util.Date());

        export = exportRepo.save(export);

        // Xóa chi tiết cũ
        detailRepo.deleteByExportId(id);

        // Tạo chi tiết mới
        BigDecimal total = BigDecimal.ZERO;
        List<ShopExportDetail> details = new ArrayList<>();

        for (ExportDetailRequest item : req.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            if (item.getUnitPrice() == null) {
                continue;
            }

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

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        export.setDetails(details);

        return toDto(export, total);
    }

    // ========= helpers ========= //

    private String generateCode() {
        // TODO: sau này thay bằng sequence / format chuẩn (PXNCC + yyyyMM + số tăng
        // dần)
        return "PXNCC" + System.currentTimeMillis();
    }

    private SupplierExportDto toDtoWithCalcTotal(ShopExport e) {
        BigDecimal total = BigDecimal.ZERO;
        List<ExportDetailDto> itemDtos = new ArrayList<>();

        if (e.getDetails() != null) {
            for (ShopExportDetail d : e.getDetails()) {
                if (d.getUnitPrice() == null || d.getQuantity() == null)
                    continue;

                BigDecimal line = d.getUnitPrice()
                        .multiply(BigDecimal.valueOf(d.getQuantity()));
                total = total.add(line);

                // Map sang DTO
                ExportDetailDto itemDto = new ExportDetailDto();
                itemDto.setId(d.getId());
                itemDto.setProductId(d.getProductId());
                itemDto.setQuantity(d.getQuantity());
                itemDto.setUnitPrice(d.getUnitPrice());
                itemDto.setImportDetailsId(d.getImportDetailsId());

                // FE sẽ tự join thông tin sản phẩm từ product-service
                itemDto.setProductCode(null);
                itemDto.setProductName(null);
                itemDto.setUnit(null);

                itemDtos.add(itemDto);
            }
        }

        SupplierExportDto dto = toDto(e, total);
        dto.setItems(itemDtos);
        return dto;
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

        // FE sẽ tự join tên NCC từ product-service hoặc bạn bổ sung sau
        dto.setSupplierName(null);
        return dto;
    }
}
