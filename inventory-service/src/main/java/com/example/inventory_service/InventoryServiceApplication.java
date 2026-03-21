package com.example.inventory_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories(
    basePackages = "com.example.inventory_service.repository",
    includeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.example.inventory_service.repository.ShopImportRepository.class,
                com.example.inventory_service.repository.ShopImportDetailRepository.class,
                com.example.inventory_service.repository.ShopExportRepository.class,
                com.example.inventory_service.repository.ShopExportDetailRepository.class,
                com.example.inventory_service.repository.ShopStoreRepository.class,
                com.example.inventory_service.repository.ShopStockRepository.class,
                com.example.inventory_service.repository.InventoryCheckRepository.class,
                com.example.inventory_service.repository.InventoryCheckDetailRepository.class
            }
        )
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.example.inventory_service.repository.UserQueryRepository.class
            }
        )
    }
)
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

}
