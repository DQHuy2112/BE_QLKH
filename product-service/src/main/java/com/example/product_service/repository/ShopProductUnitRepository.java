package com.example.product_service.repository;

import com.example.product_service.entity.ShopProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShopProductUnitRepository extends JpaRepository<ShopProductUnit, Long>, JpaSpecificationExecutor<ShopProductUnit> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}


