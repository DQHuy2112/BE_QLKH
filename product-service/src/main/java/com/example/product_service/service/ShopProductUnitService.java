package com.example.product_service.service;

import com.example.product_service.dto.UnitDto;
import com.example.product_service.dto.UnitRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ShopProductUnitService {

    List<UnitDto> getAll();

    UnitDto getById(Long id);

    UnitDto create(UnitRequest request);

    UnitDto update(Long id, UnitRequest request);

    void delete(Long id);

    Page<UnitDto> search(String name, Pageable pageable);
}


