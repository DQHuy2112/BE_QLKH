# Migration: Thêm cột confirmed_by và confirmed_at vào bảng shop_inventory_checks

## Mô tả
Migration này thêm 4 cột mới vào bảng `shop_inventory_checks` để hỗ trợ quy trình 2 cấp và audit trail:
- **confirmed_by**: ID người xác nhận cuối cùng (Admin)
- **confirmed_at**: Thời gian xác nhận cuối cùng
- **rejected_by**: ID người từ chối
- **rejected_at**: Thời gian từ chối

## Quy trình 2 cấp
1. **Manager duyệt**: `PENDING` → `APPROVED` (set `approved_by`, `approved_at`)
2. **Admin xác nhận**: `APPROVED` → cập nhật tồn kho (set `confirmed_by`, `confirmed_at`)

## Cách chạy migration

### Cách 1: Sử dụng file đơn giản (MySQL 8.0.19+)
```bash
mysql -u your_username -p your_database_name < migration_add_confirmed_fields_simple.sql
```

### Cách 2: Sử dụng file với prepared statement (Tất cả MySQL versions)
```bash
mysql -u your_username -p your_database_name < migration_add_confirmed_fields_to_inventory_checks.sql
```

**Lưu ý**: Nhớ thay đổi `your_database_name` trong file trước khi chạy, hoặc sử dụng:
```bash
mysql -u your_username -p -D your_database_name < migration_add_confirmed_fields_to_inventory_checks.sql
```

### Cách 3: Chạy thủ công từ MySQL client
```sql
USE your_database_name;

-- Thêm cột confirmed_by
ALTER TABLE `shop_inventory_checks` 
ADD COLUMN `confirmed_by` BIGINT NULL COMMENT 'ID người xác nhận cuối cùng (Admin)' 
AFTER `approved_at`;

-- Thêm cột confirmed_at
ALTER TABLE `shop_inventory_checks` 
ADD COLUMN `confirmed_at` DATETIME NULL COMMENT 'Thời gian xác nhận cuối cùng' 
AFTER `confirmed_by`;
```

## Kiểm tra kết quả
Sau khi chạy migration, kiểm tra bằng lệnh:
```sql
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'shop_inventory_checks' 
  AND COLUMN_NAME IN ('confirmed_by', 'confirmed_at');
```

Kết quả mong đợi:
- `confirmed_by`: BIGINT, NULL, 'ID người xác nhận cuối cùng (Admin)'
- `confirmed_at`: DATETIME, NULL, 'Thời gian xác nhận cuối cùng'
- `rejected_by`: BIGINT, NULL, 'ID người từ chối'
- `rejected_at`: DATETIME, NULL, 'Thời gian từ chối'

## Rollback (nếu cần)
```sql
ALTER TABLE `shop_inventory_checks` DROP COLUMN `rejected_at`;
ALTER TABLE `shop_inventory_checks` DROP COLUMN `rejected_by`;
ALTER TABLE `shop_inventory_checks` DROP COLUMN `confirmed_at`;
ALTER TABLE `shop_inventory_checks` DROP COLUMN `confirmed_by`;
```

## Lưu ý
- Migration này **an toàn** để chạy nhiều lần (có kiểm tra cột đã tồn tại)
- Không ảnh hưởng đến dữ liệu hiện có
- Các giá trị mặc định là NULL (phù hợp với dữ liệu cũ)


