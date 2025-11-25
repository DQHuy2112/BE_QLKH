package com.example.inventory_service.service.impl;

import com.example.inventory_service.client.ProductServiceClient;
import com.example.inventory_service.dto.ImportDetailDto;
import com.example.inventory_service.dto.ImportDetailRequest;
import com.example.inventory_service.dto.SupplierImportDto;
import com.example.inventory_service.dto.SupplierImportRequest;
import com.example.inventory_service.entity.ShopImport;
import com.example.inventory_service.entity.ShopImportDetail;
import com.example.inventory_service.exception.NotFoundException;
import com.example.inventory_service.repository.ShopImportDetailRepository;
import com.example.inventory_service.repository.ShopImportRepository;
import com.example.inventory_service.service.StaffImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý phiếu nhập từ nhân viên (STAFF)
 * Nhân viên nhập hàng trực tiếp (không qua NCC hay kho khác)
 */
@Service
public class StaffImportServiceImpl implements StaffImportService {

    private final ShopImportRepository importRepo;
    private final ShopImportDetailRepository detailRepo;
    private final ProductServiceClient productClient;
    private final com.example.inventory_service.repository.ShopStoreRepository storeRepo;

    public StaffImportServiceImpl(
            ShopImportRepository importRepo,
            ShopImportDetailRepository detailRepo,
            ProductServiceClient productClient,
            com.example.inventory_service.repository.ShopStoreRepository storeRepo) {
        this.importRepo = importRepo;
        this.detailRepo = detailRepo;
        this.productClient = productClient;
        this.storeRepo = storeRepo;
    }

    @Override
    @Transactional
    public SupplierImportDto create(SupplierImportRequest request) {
        java.util.Date now = new java.util.Date();

        ShopImport im = new ShopImport();

        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        } else {
            im.setCode(generateCode());
        }

        im.setImportType("STAFF"); // ⭐ Phiếu nhập từ nhân viên
        im.setStoreId(request.getStoreId());
        // Với phiếu nhân viên, supplierId lưu staffId (ID nhân viên)
        im.setSupplierId(request.getStaffId() != null ? request.getStaffId() : request.getSupplierId());
        im.setNote(limitNote(request.getNote()));
        im.setDescription(request.getDescription());
        im.setStatus("PENDING");
        im.setImportsDate(now);
        im.setCreatedAt(now);
        im.setUpdatedAt(now);

        // Lưu ảnh
        if (request.getAttachmentImages() != null && !request.getAttachmentImages().isEmpty()) {
            String joined = request.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            im.setAttachmentImage(joined);
        } else {
            im.setAttachmentImage(null);
        }

        im = importRepo.save(im);

        // Lưu chi tiết
        BigDecimal total = BigDecimal.ZERO;
        List<ShopImportDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (ImportDetailRequest item : request.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0)
                    continue;
                if (item.getUnitPrice() == null)
                    continue;

                ShopImportDetail d = new ShopImportDetail();
                d.setImportId(im.getId());
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO);

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(im, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierImportDto> search(
            String status,
            String code,
            LocalDate from,
            LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to.plusDays(1)) : null;

        // Chỉ lấy phiếu STAFF
        List<ShopImport> list = importRepo.searchStaffImports(
                status,
                code,
                fromDate,
                toDate);

        List<SupplierImportDto> result = new ArrayList<>();
        for (ShopImport im : list) {
            result.add(toDtoWithCalcTotal(im));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierImportDto getById(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff import not found: " + id));

        // Kiểm tra có phải STAFF không
        if (!"STAFF".equals(im.getImportType())) {
            throw new IllegalStateException("This is not a staff import");
        }

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto update(Long id, SupplierImportRequest request) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff import not found: " + id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        }
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getStaffId() != null ? request.getStaffId() : request.getSupplierId());
        im.setNote(limitNote(request.getNote()));
        im.setDescription(request.getDescription());
        im.setUpdatedAt(new java.util.Date());

        // Cập nhật ảnh
        if (request.getAttachmentImages() != null && !request.getAttachmentImages().isEmpty()) {
            String joined = request.getAttachmentImages().stream()
                    .map(this::normalizeImagePath)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(";"));
            im.setAttachmentImage(joined);
        } else {
            im.setAttachmentImage(null);
        }

        im = importRepo.save(im);

        // Xóa chi tiết cũ và tạo mới
        detailRepo.deleteByImportId(id);

        BigDecimal total = BigDecimal.ZERO;
        List<ShopImportDetail> details = new ArrayList<>();

        if (request.getItems() != null) {
            for (ImportDetailRequest item : request.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0)
                    continue;
                if (item.getUnitPrice() == null)
                    continue;

                ShopImportDetail d = new ShopImportDetail();
                d.setImportId(im.getId());
                d.setProductId(item.getProductId());
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(item.getUnitPrice());
                d.setDiscountPercent(item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO);

                details.add(d);

                BigDecimal line = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(line);
            }
        }

        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }

        return toDto(im, total);
    }

    @Override
    @Transactional
    public SupplierImportDto confirm(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff import not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xác nhận phiếu đang ở trạng thái PENDING");
        }

        im.setStatus("IMPORTED");
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        // Cập nhật tồn kho
        List<ShopImportDetail> details = detailRepo.findByImportId(id);
        for (ShopImportDetail d : details) {
            if (d.getQuantity() != null && d.getQuantity() > 0) {
                productClient.increaseQuantity(d.getProductId(), d.getQuantity());
            }
        }

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto cancel(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff import not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy phiếu đang ở trạng thái PENDING");
        }

        im.setStatus("CANCELLED");
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        return toDtoWithCalcTotal(im);
    }

    // ========= HELPER METHODS ========= //

    private String generateCode() {
        return "PNNV" + System.currentTimeMillis(); // Phiếu Nhập Nhân Viên
    }

    private String limitNote(String note) {
        if (note == null)
            return null;
        int max = 255;
        return note.length() > max ? note.substring(0, max) : note;
    }

    private String normalizeImagePath(String raw) {
        if (raw == null || raw.isBlank())
            return null;

        int idx = raw.indexOf("/uploads/");
        if (idx >= 0) {
            return raw.substring(idx);
        }

        if (!raw.startsWith("/")) {
            return "/" + raw;
        }

        return raw;
    }

    private SupplierImportDto toDtoWithCalcTotal(ShopImport im) {
        List<ShopImportDetail> details = detailRepo.findByImportId(im.getId());
        BigDecimal total = BigDecimal.ZERO;
        List<ImportDetailDto> itemDtos = new ArrayList<>();

        if (details != null) {
            for (ShopImportDetail d : details) {
                if (d.getUnitPrice() == null || d.getQuantity() == null)
                    continue;

                BigDecimal line = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
                total = total.add(line);

                ImportDetailDto itemDto = new ImportDetailDto();
                itemDto.setId(d.getId());
                itemDto.setProductId(d.getProductId());
                itemDto.setQuantity(d.getQuantity());
                itemDto.setUnitPrice(d.getUnitPrice());
                itemDto.setDiscountPercent(d.getDiscountPercent());
                itemDto.setProductCode(null);
                itemDto.setProductName(null);
                itemDto.setUnit(null);

                itemDtos.add(itemDto);
            }
        }

        SupplierImportDto dto = toDto(im, total);
        dto.setItems(itemDtos);
        return dto;
    }

    private SupplierImportDto toDto(ShopImport imp, BigDecimal total) {
        SupplierImportDto dto = new SupplierImportDto();
        dto.setId(imp.getId());
        dto.setCode(imp.getCode());
        dto.setStoreId(imp.getStoreId());
        dto.setSupplierId(imp.getSupplierId()); // Đây là staffId cho phiếu nhân viên
        dto.setStatus(imp.getStatus());
        dto.setImportsDate(imp.getImportsDate());
        dto.setNote(imp.getNote());
        dto.setTotalValue(total);

        // Lấy thông tin kho
        if (imp.getStoreId() != null) {
            storeRepo.findById(imp.getStoreId()).ifPresent(store -> {
                dto.setStoreName(store.getName());
            });
        }

        // Lấy thông tin nhân viên (supplierId trong phiếu nhân viên là staffId)
        if (imp.getSupplierId() != null) {
            dto.setStaffId(imp.getSupplierId());
            // TODO: Gọi API lấy thông tin nhân viên từ supplier service
            // Tạm thời hiển thị ID
            dto.setSupplierName("Nhân viên #" + imp.getSupplierId());
            dto.setSupplierCode("STAFF" + imp.getSupplierId());
        } else {
            dto.setSupplierName(null);
            dto.setSupplierCode(null);
        }

        // Map ảnh
        List<String> images = new ArrayList<>();
        String raw = imp.getAttachmentImage();
        if (raw != null && !raw.isBlank()) {
            images = Arrays.stream(raw.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        dto.setAttachmentImages(images);

        return dto;
    }
}
