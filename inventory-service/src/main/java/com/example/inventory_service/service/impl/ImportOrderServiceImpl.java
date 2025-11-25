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
import com.example.inventory_service.service.ImportOrderService;
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
 * Service xử lý lệnh nhập kho (ORDER)
 * Khác với phiếu nhập: Lệnh chỉ là đặt hàng, chưa nhập kho thật
 * import_type = "ORDER"
 */
@Service
public class ImportOrderServiceImpl implements ImportOrderService {

    private final ShopImportRepository importRepo;
    private final ShopImportDetailRepository detailRepo;
    private final ProductServiceClient productServiceClient;

    public ImportOrderServiceImpl(
            ShopImportRepository importRepo,
            ShopImportDetailRepository detailRepo,
            ProductServiceClient productServiceClient) {
        this.importRepo = importRepo;
        this.detailRepo = detailRepo;
        this.productServiceClient = productServiceClient;
    }

    @Override
    @Transactional
    public SupplierImportDto create(SupplierImportRequest request) {
        // ⭐ Validation: Bắt buộc phải có supplierId cho Import ORDER
        if (request.getSupplierId() == null) {
            throw new IllegalArgumentException("supplierId is required for Import ORDER");
        }

        java.util.Date now = new java.util.Date();

        ShopImport im = new ShopImport();

        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        } else {
            im.setCode(generateCode());
        }

        im.setImportType("ORDER"); // ⭐ Lệnh nhập kho
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getSupplierId());
        im.setNote(limitNote(request.getNote()));
        im.setDescription(request.getDescription());
        im.setStatus("PENDING"); // Chờ duyệt
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

        // Chỉ lấy lệnh ORDER
        List<ShopImport> list = importRepo.searchImportOrders(
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
                .orElseThrow(() -> new NotFoundException("Import order not found: " + id));

        // Kiểm tra có phải ORDER không
        if (!"ORDER".equals(im.getImportType())) {
            throw new IllegalStateException("This is not an import order");
        }

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto update(Long id, SupplierImportRequest request) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import order not found: " + id));

        if (request.getCode() != null && !request.getCode().isBlank()) {
            im.setCode(request.getCode());
        }
        im.setStoreId(request.getStoreId());
        im.setSupplierId(request.getSupplierId());
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
    public SupplierImportDto approve(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import order not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể duyệt lệnh đang ở trạng thái PENDING");
        }

        // Duyệt lệnh → APPROVED (chưa nhập kho, không cập nhật tồn)
        im.setStatus("APPROVED");
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        return toDtoWithCalcTotal(im);
    }

    @Override
    @Transactional
    public SupplierImportDto cancel(Long id) {
        ShopImport im = importRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Import order not found: " + id));

        if (!"PENDING".equals(im.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy lệnh đang ở trạng thái PENDING");
        }

        im.setStatus("CANCELLED");
        im.setUpdatedAt(new java.util.Date());
        im = importRepo.save(im);

        return toDtoWithCalcTotal(im);
    }

    // ========= HELPER METHODS ========= //

    private String generateCode() {
        return "LNK" + System.currentTimeMillis(); // Lệnh Nhập Kho
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
        dto.setSupplierId(imp.getSupplierId());
        dto.setStatus(imp.getStatus());
        dto.setImportsDate(imp.getImportsDate());
        dto.setNote(imp.getNote());
        dto.setTotalValue(total);

        // Load supplier info (name, code, phone, address)
        if (imp.getSupplierId() != null) {
            try {
                ProductServiceClient.SupplierDto supplier = productServiceClient.getSupplier(imp.getSupplierId());
                if (supplier != null) {
                    dto.setSupplierName(supplier.getName());
                    dto.setSupplierCode(supplier.getCode());
                    dto.setSupplierPhone(supplier.getPhone());
                    dto.setSupplierAddress(supplier.getAddress());
                }
            } catch (Exception e) {
                System.err.println("Failed to load supplier info: " + e.getMessage());
                dto.setSupplierName(null);
                dto.setSupplierCode(null);
                dto.setSupplierPhone(null);
                dto.setSupplierAddress(null);
            }
        } else {
            dto.setSupplierName(null);
            dto.setSupplierCode(null);
            dto.setSupplierPhone(null);
            dto.setSupplierAddress(null);
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
