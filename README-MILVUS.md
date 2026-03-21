# Hướng dẫn sử dụng Milvus với Docker

## ✅ Không cần cài Milvus bên ngoài!

Milvus sẽ tự động được cài đặt và chạy trong Docker container khi bạn chạy `docker-compose up`.

## 📋 Yêu cầu

1. **Docker Desktop** (Windows) - [Download tại đây](https://www.docker.com/products/docker-desktop)
2. **Docker Compose** (thường đi kèm với Docker Desktop)

## 🚀 Cách chạy

### Option 1: Chạy trực tiếp (Khuyến nghị)
```bash
# Build images (nếu cần)
docker compose build

# Chạy tất cả services (bao gồm Milvus)
docker-compose up -d

# Hoặc chỉ chạy Milvus và dependencies
docker-compose up -d etcd minio milvus-standalone
```

### Option 2: Chạy script tự động (Windows)
```bash
start-milvus.bat
```

## 🔍 Kiểm tra Milvus đã chạy

```bash
# Xem logs
docker-compose logs -f milvus-standalone

# Kiểm tra status
docker-compose ps milvus-standalone

# Test kết nối (nếu có curl)
curl http://localhost:9091/healthz
```

## 📊 Services được tạo

1. **milvus-standalone**: Milvus server (port 19530, 9091)
2. **etcd**: Metadata storage cho Milvus
3. **minio**: Object storage cho Milvus (port 9000, 9001)

## 🔧 Cấu hình

Milvus sẽ tự động được cấu hình qua environment variables trong `docker-compose.yml`:
- `MILVUS_HOST: milvus-standalone` (tên service trong Docker network)
- `MILVUS_PORT: 19530`

## 💾 Dữ liệu được lưu ở đâu?

Dữ liệu được lưu trong volumes:
- `./volumes/milvus` - Dữ liệu Milvus
- `./volumes/etcd` - Metadata
- `./volumes/minio` - Object storage

## 🛑 Dừng services

```bash
docker-compose stop milvus-standalone etcd minio
```

## 🗑️ Xóa dữ liệu (nếu cần reset)

**⚠️ QUAN TRỌNG: Phải dừng container trước khi xóa volumes!**

```bash
# Bước 1: Dừng tất cả containers
docker-compose down

# Bước 2: Xóa thư mục volumes trên máy (sẽ mất hết dữ liệu!)
# Windows PowerShell:
Remove-Item -Recurse -Force .\volumes\milvus
Remove-Item -Recurse -Force .\volumes\etcd
Remove-Item -Recurse -Force .\volumes\minio

# Hoặc Windows CMD:
rmdir /s /q volumes\milvus
rmdir /s /q volumes\etcd
rmdir /s /q volumes\minio
```

**Lưu ý:**
- Volumes được lưu trực tiếp trong thư mục `./volumes/` trên máy bạn
- Nếu container đang chạy, **KHÔNG** xóa volumes (có thể gây lỗi hoặc mất dữ liệu)
- Sau khi xóa, chạy lại `docker-compose up -d` để tạo volumes mới

## ⚠️ Lưu ý

- Lần đầu chạy sẽ mất vài phút để download images
- Đảm bảo ports 19530, 9091, 9000, 9001 không bị chiếm bởi ứng dụng khác
- Nếu ai-service chạy ngoài Docker, cần set `MILVUS_HOST=localhost` thay vì `milvus-standalone`
