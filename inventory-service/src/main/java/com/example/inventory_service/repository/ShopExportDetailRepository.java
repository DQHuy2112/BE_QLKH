package com.example.inventory_service.repository;

import com.example.inventory_service.entity.ShopExportDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopExportDetailRepository extends JpaRepository<ShopExportDetail, Long> {

    List<ShopExportDetail> findByExportId(Long exportId);

    // dùng cho tính tồn kho
    List<ShopExportDetail> findByProductId(Long productId);

    // xóa chi tiết theo exportId (dùng cho update)
    void deleteByExportId(Long exportId);
}
