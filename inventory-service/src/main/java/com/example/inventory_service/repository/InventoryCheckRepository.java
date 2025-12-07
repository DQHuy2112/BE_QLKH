package com.example.inventory_service.repository;

import com.example.inventory_service.entity.InventoryCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {

    @Query("""
            SELECT ic FROM InventoryCheck ic
            WHERE (:status IS NULL OR ic.status = :status)
              AND (:checkCode IS NULL OR ic.checkCode LIKE CONCAT('%', :checkCode, '%'))
              AND (:fromDate IS NULL OR ic.checkDate >= :fromDate)
              AND (:toDate IS NULL OR ic.checkDate < :toDate)
            ORDER BY ic.checkDate DESC
            """)
    List<InventoryCheck> searchInventoryChecks(
            @Param("status") String status,
            @Param("checkCode") String checkCode,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate);

    @Query("""
            SELECT ic FROM InventoryCheck ic
            WHERE (:status IS NULL OR ic.status = :status)
              AND (:checkCode IS NULL OR ic.checkCode LIKE CONCAT('%', :checkCode, '%'))
              AND (:fromDate IS NULL OR ic.checkDate >= :fromDate)
              AND (:toDate IS NULL OR ic.checkDate < :toDate)
            ORDER BY ic.checkDate DESC
            """)
    Page<InventoryCheck> searchInventoryChecksPaged(
            @Param("status") String status,
            @Param("checkCode") String checkCode,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            Pageable pageable);

    List<InventoryCheck> findByStoreId(Long storeId);
}
