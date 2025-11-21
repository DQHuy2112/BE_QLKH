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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierExportServiceImpl implements SupplierExportService {

    private final ShopExportRepository exportRepo;
    private final ShopExportDetailRepository detailRepo;
    private final com.example.inventory_service.service.StockService stockService;
    private final com.example.inventory_service.client.ProductServiceClient productClient;

    public SupplierExportServiceImpl(ShopExportRepository exportRepo,
            ShopExportDetailRepository detailRepo,
            com.example.inventory_service.service.StockService stockService,
            com.example.inventory_service.client.ProductServiceClient productClient) {
        this.exportRepo = exportRepo;
        this.detailRepo = detailRepo;
        this.stockService = stockService;
        this.productClient = productClient;
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

        // ===== LƯU NHIỀU ẢNH VÀO 1 CỘT attachment_image =====
        if (req.getAttachmentImages() != null && !req.getAttachmentImages().isEmpty()) {
            String joined = req.getAttachmentImages().stream()
                    .map(this::normalizeImagePath) // chuẩn hoá /uploads/...
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";")); // path1;path2;path3

            export.setAttachmentImage(joined);
        } else {
            export.setAttachmentImage(null);
        }

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
            d.setExportId(export.getId());
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

        // Cập nhật ảnh
        if (req.getAttachmentImages() != null && !req.getAttachmentImages().isEmpty()) {
            String joined = req.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            export.setAttachmentImage(joined);
        } else {
            export.setAttachmentImage(null);
        }

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
            d.setExportId(export.getId());
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

        // Lấy chi tiết từ repository
        List<ShopExportDetail> details = detailRepo.findByExportId(e.getId());

        if (details != null) {
            for (ShopExportDetail d : details) {
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

        // ==== map nhiều ảnh từ 1 cột attachment_image ====
        List<String> images = new ArrayList<>();
        String raw = e.getAttachmentImage();
        if (raw != null && !raw.isBlank()) {
            images = Arrays.stream(raw.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        dto.setAttachmentImages(images);

        return dto;
    }

    // luôn lưu dạng /uploads/... để khớp WebConfig + FE
    private String normalizeImagePath(String raw) {
        if (raw == null || raw.isBlank())
            return null;

        // nếu FE gửi full URL http://.../uploads/...
        int idx = raw.indexOf("/uploads/");
        if (idx >= 0) {
            return raw.substring(idx);
        }

        // nếu FE gửi tương đối mà thiếu dấu /
        if (!raw.startsWith("/")) {
            return "/" + raw;
        }

        return raw;
    }

    // =========================================================
    // XÁC NHẬN XUẤT KHO (PENDING → EXPORTED)
    // =========================================================
    @Override
    @Transactional
    public SupplierExportDto confirm(Long id) {
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        // Chỉ cho phép confirm khi đang PENDING
        if (!"PENDING".equals(export.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xác nhận phiếu đang ở trạng thái PENDING");
        }

        // Kiểm tra tồn kho trước khi xuất
        List<ShopExportDetail> details = detailRepo.findByExportId(id);
        for (ShopExportDetail d : details) {
            if (!stockService.hasEnoughStock(d.getProductId(), d.getQuantity())) {
                int current = stockService.getCurrentStock(d.getProductId());
                throw new IllegalStateException(
                        String.format("Sản phẩm ID %d không đủ số lượng trong kho. Tồn: %d, Cần: %d",
                                d.getProductId(), current, d.getQuantity()));
            }
        }

        // Cập nhật trạng thái
        export.setStatus("EXPORTED");
        export.setUpdatedAt(new java.util.Date());
        export = exportRepo.save(export);

        // Cập nhật tồn kho: Giảm số lượng theo từng sản phẩm trong phiếu
        for (ShopExportDetail d : details) {
            if (d.getQuantity() != null && d.getQuantity() > 0) {
                productClient.decreaseQuantity(d.getProductId(), d.getQuantity());
            }
        }

        return toDtoWithCalcTotal(export);
    }

    // =========================================================
    // HỦY PHIẾU XUẤT (PENDING → CANCELLED)
    // =========================================================
    @Override
    @Transactional
    public SupplierExportDto cancel(Long id) {
        ShopExport export = exportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Export not found: " + id));

        // Chỉ cho phép hủy khi đang PENDING
        if (!"PENDING".equals(export.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy phiếu đang ở trạng thái PENDING");
        }

        // Cập nhật trạng thái
        export.setStatus("CANCELLED");
        export.setUpdatedAt(new java.util.Date());
        export = exportRepo.save(export);

        return toDtoWithCalcTotal(export);
    }

}
