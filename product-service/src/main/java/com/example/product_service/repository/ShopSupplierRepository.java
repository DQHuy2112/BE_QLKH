package com.example.product_service.repository;

import com.example.product_service.entity.ShopSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopSupplierRepository extends JpaRepository<ShopSupplier, Long> {
    List<ShopSupplier> findByType(String type);
}
