package com.example.inventory_service.repository;

import com.example.inventory_service.entity.ShopStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ShopStoreRepository extends JpaRepository<ShopStore, Long>, JpaSpecificationExecutor<ShopStore> {
    List<ShopStore> findByCodeStartingWith(String prefix);
}
