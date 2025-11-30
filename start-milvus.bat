@echo off
echo ========================================
echo Starting Milvus and dependencies...
echo ========================================
echo.

REM Check if Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

echo Docker is running. Starting Milvus services...
echo.

REM Start Milvus and dependencies
docker-compose up -d etcd minio milvus-standalone

echo.
echo Waiting for services to be ready...
timeout /t 10 /nobreak >nul

echo.
echo Checking service status...
docker-compose ps etcd minio milvus-standalone

echo.
echo ========================================
echo Milvus services started!
echo ========================================
echo.
echo Milvus is available at: localhost:19530
echo MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
echo.
echo To view logs: docker-compose logs -f milvus-standalone
echo To stop: docker-compose stop milvus-standalone etcd minio
echo.
pause
