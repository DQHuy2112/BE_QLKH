package com.example.product_service.repository;

import com.example.product_service.entity.ShopSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ShopSupplierRepository extends JpaRepository<ShopSupplier, Long>, JpaSpecificationExecutor<ShopSupplier> {
    List<ShopSupplier> findByType(String type);
    List<ShopSupplier> findByCodeStartingWith(String prefix);
}
