package com.example.product_service.repository;

import com.example.product_service.entity.ShopCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShopCategoryRepository
        extends JpaRepository<ShopCategory, Long>,
                JpaSpecificationExecutor<ShopCategory> {
}
