package com.example.order_service.repository;

import com.example.order_service.entity.ShopCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ShopCustomerRepository extends JpaRepository<ShopCustomer, Long>, JpaSpecificationExecutor<ShopCustomer> {
    List<ShopCustomer> findByCodeStartingWith(String prefix);
}
