package com.example.order_service.repository;

import com.example.order_service.entity.ShopCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopCustomerRepository extends JpaRepository<ShopCustomer, Long> {
    List<ShopCustomer> findByCodeStartingWith(String prefix);
}
