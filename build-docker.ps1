# Script tự động build Docker cho các services đã thay đổi
# Chạy: .\build-docker.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building Docker Images for Updated Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$services = @(
    @{Name="order-service"; Port=8083},
    @{Name="product-service"; Port=8081},
    @{Name="inventory-service"; Port=8082}
)

foreach ($service in $services) {
    $serviceName = $service.Name
    $servicePath = ".\$serviceName"
    
    Write-Host "`n----------------------------------------" -ForegroundColor Yellow
    Write-Host "Building $serviceName..." -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    
    # Bước 1: Build Maven
    Write-Host "Step 1: Building Maven project..." -ForegroundColor Green
    Push-Location $servicePath
    try {
        .\mvnw.cmd clean package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Maven build failed for $serviceName" -ForegroundColor Red
            Pop-Location
            continue
        }
        Write-Host "Maven build successful!" -ForegroundColor Green
    }
    catch {
        Write-Host "Error building Maven: $_" -ForegroundColor Red
        Pop-Location
        continue
    }
    Pop-Location
    
    # Bước 2: Build Docker image
    Write-Host "Step 2: Building Docker image..." -ForegroundColor Green
    try {
        docker build -t $serviceName`:latest $servicePath
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Docker build failed for $serviceName" -ForegroundColor Red
            continue
        }
        Write-Host "Docker image built successfully!" -ForegroundColor Green
    }
    catch {
        Write-Host "Error building Docker: $_" -ForegroundColor Red
        continue
    }
    
    Write-Host "$serviceName build completed!" -ForegroundColor Cyan
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "All builds completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`nTo restart services, run:" -ForegroundColor Yellow
Write-Host "docker-compose up -d order-service product-service inventory-service" -ForegroundColor White

