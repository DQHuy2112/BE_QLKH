# Script to rebuild auth-service
Write-Host "=== Rebuilding auth-service ===" -ForegroundColor Green

# Navigate to auth-service directory
Set-Location "E:\DACN\DACN_QLKH\BE_QLKH\auth-service"

# Check if Maven is available
$mvnPath = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnPath) {
    Write-Host "Maven not found in PATH. Please build manually or install Maven." -ForegroundColor Yellow
    Write-Host "You can build using your IDE (IntelliJ IDEA/Eclipse) or install Maven." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To build manually:" -ForegroundColor Cyan
    Write-Host "1. Open auth-service project in your IDE" -ForegroundColor Cyan
    Write-Host "2. Right-click on project -> Maven -> Reload project" -ForegroundColor Cyan
    Write-Host "3. Right-click on project -> Maven -> Lifecycle -> clean" -ForegroundColor Cyan
    Write-Host "4. Right-click on project -> Maven -> Lifecycle -> package (skip tests)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "After building, run:" -ForegroundColor Cyan
    Write-Host "  docker-compose build auth-service" -ForegroundColor Cyan
    Write-Host "  docker-compose up -d auth-service" -ForegroundColor Cyan
    exit 1
}

# Build Maven project
Write-Host "Building Maven project..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed!" -ForegroundColor Red
    exit 1
}

# Navigate back to root
Set-Location "E:\DACN\DACN_QLKH\BE_QLKH"

# Rebuild Docker image
Write-Host "Rebuilding Docker image..." -ForegroundColor Yellow
docker-compose build auth-service

# Restart container
Write-Host "Restarting container..." -ForegroundColor Yellow
docker-compose up -d auth-service

Write-Host "Done! Check logs with: docker-compose logs auth-service --tail 50" -ForegroundColor Green
