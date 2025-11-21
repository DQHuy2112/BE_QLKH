package com.example.product_service.service.impl;

import com.example.product_service.dto.ProductDto;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.entity.ShopProduct;
import com.example.product_service.exception.NotFoundException;
import com.example.product_service.repository.ShopCategoryRepository;
import com.example.product_service.repository.ShopProductRepository;
import com.example.product_service.service.ShopProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class ShopProductServiceImpl implements ShopProductService {

    private final ShopProductRepository repo;
    private final ShopCategoryRepository categoryRepo;

    // ✅ Constructor duy nhất, tiêm cả 2 repo
    public ShopProductServiceImpl(ShopProductRepository repo,
            ShopCategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @Override
    public List<ProductDto> getAll() {
        return repo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ProductDto getById(Long id) {
        ShopProduct p = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        return toDto(p);
    }

    @Override
    public ProductDto create(ProductRequest request) {
        ShopProduct p = new ShopProduct();
        applyRequestToEntity(request, p);
        p.setCreatedAt(new Date());
        p.setUpdatedAt(new Date());
        return toDto(repo.save(p));
    }

    @Override
    public ProductDto update(Long id, ProductRequest request) {
        ShopProduct p = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        applyRequestToEntity(request, p);
        p.setUpdatedAt(new Date());
        return toDto(repo.save(p));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Product not found: " + id);
        }
        repo.deleteById(id);
    }

    // ✅ search + phân trang
    @Override
    public Page<ProductDto> search(String code,
            String name,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        Specification<ShopProduct> spec = Specification.where(null);

        if (code != null && !code.isBlank()) {
            String likeCode = "%" + code.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("code")), likeCode));
        }

        if (name != null && !name.isBlank()) {
            String likeName = "%" + name.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), likeName));
        }

        if (fromDate != null) {
            Date from = Date.from(
                    fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (toDate != null) {
            Date to = Date.from(
                    toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
        }

        return repo.findAll(spec, pageable)
                .map(this::toDto);
    }

    // ---------- mapping helpers ----------

    private ProductDto toDto(ShopProduct p) {
        ProductDto dto = new ProductDto();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setShortDescription(p.getShortDescription());
        dto.setImage(p.getImage());
        dto.setUnitPrice(p.getUnitPrice());
        dto.setQuantity(p.getQuantity());
        dto.setMinStock(p.getMinStock()); // 👈 THÊM
        dto.setMaxStock(p.getMaxStock()); // 👈 THÊM
        dto.setStatus(p.getStatus());
        dto.setCategoryId(p.getCategoryId());
        dto.setSupplierId(p.getSupplierId());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());

        // ⭐ LẤY CATEGORY NAME
        if (p.getCategoryId() != null) {
            categoryRepo.findById(p.getCategoryId())
                    .ifPresent(cat -> dto.setCategoryName(cat.getName()));
        }

        return dto;
    }

    private void applyRequestToEntity(ProductRequest req, ShopProduct p) {
        p.setCode(req.getCode());
        p.setName(req.getName());
        p.setShortDescription(req.getShortDescription());
        p.setImage(req.getImage());
        p.setUnitPrice(req.getUnitPrice());

        // quantity có thể null → default 0
        p.setQuantity(req.getQuantity() != null ? req.getQuantity() : 0);

        // 👇 THÊM 2 field tồn kho
        p.setMinStock(req.getMinStock());
        p.setMaxStock(req.getMaxStock());

        p.setStatus(req.getStatus());
        p.setCategoryId(req.getCategoryId());
        p.setSupplierId(req.getSupplierId());
    }

    // =========================================================
    // CẬP NHẬT TỒN KHO (Gọi từ Inventory-service)
    // =========================================================

    @Override
    @Transactional
    public void increaseQuantity(Long id, int amount) {
        System.out.println("🟢 Increasing quantity for product ID: " + id + ", amount: " + amount);

        ShopProduct product = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        int currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
        int newQty = currentQty + amount;

        System.out.println("📦 Current quantity: " + currentQty + " → New quantity: " + newQty);

        product.setQuantity(newQty);
        product.setUpdatedAt(new java.util.Date());

        repo.save(product);

        System.out.println("✅ Successfully updated quantity in database");
    }

    @Override
    @Transactional
    public void decreaseQuantity(Long id, int amount) {
        System.out.println("🔴 Decreasing quantity for product ID: " + id + ", amount: " + amount);

        ShopProduct product = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        int currentQty = product.getQuantity() != null ? product.getQuantity() : 0;
        int newQty = currentQty - amount;

        System.out.println("📦 Current quantity: " + currentQty + " → New quantity: " + newQty);

        // Không cho phép tồn kho âm
        if (newQty < 0) {
            System.err.println("❌ Not enough stock! Current: " + currentQty + ", Need: " + amount);
            throw new IllegalStateException(
                    String.format("Không đủ tồn kho. Hiện tại: %d, Cần giảm: %d", currentQty, amount));
        }

        product.setQuantity(newQty);
        product.setUpdatedAt(new java.util.Date());

        repo.save(product);

        System.out.println("✅ Successfully updated quantity in database");
    }

    @Override
    @Transactional
    public void updateQuantity(Long id, int quantity) {
        ShopProduct product = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        product.setQuantity(quantity);
        product.setUpdatedAt(new java.util.Date());

        repo.save(product);
    }

}
