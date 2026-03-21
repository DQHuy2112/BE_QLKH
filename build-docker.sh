#!/bin/bash
# Script tự động build Docker cho các services đã thay đổi
# Chạy: ./build-docker.sh

echo "========================================"
echo "Building Docker Images for Updated Services"
echo "========================================"

services=("order-service" "product-service" "inventory-service")

for service in "${services[@]}"; do
    echo ""
    echo "----------------------------------------"
    echo "Building $service..."
    echo "----------------------------------------"
    
    # Bước 1: Build Maven
    echo "Step 1: Building Maven project..."
    cd "$service"
    if ! ./mvnw clean package -DskipTests; then
        echo "Maven build failed for $service"
        cd ..
        continue
    fi
    echo "Maven build successful!"
    cd ..
    
    # Bước 2: Build Docker image
    echo "Step 2: Building Docker image..."
    if ! docker build -t "$service:latest" "./$service"; then
        echo "Docker build failed for $service"
        continue
    fi
    echo "Docker image built successfully!"
    
    echo "$service build completed!"
done

echo ""
echo "========================================"
echo "All builds completed!"
echo "========================================"
echo ""
echo "To restart services, run:"
echo "docker-compose up -d order-service product-service inventory-service"

