# Hướng dẫn sửa lỗi DELETE Activity Logs Endpoint

## Vấn đề
Frontend nhận lỗi 404 khi gọi DELETE endpoint cho activity logs:
- `DELETE /api/activity-logs/{id}` → 404 Not Found
- `DELETE /api/activity-logs/bulk` → 404 Not Found

## Nguyên nhân có thể
1. Backend chưa được restart sau khi thêm code
2. Endpoint chưa được deploy
3. Vấn đề về routing hoặc gateway
4. Permission issue (nhưng thường sẽ là 403, không phải 404)

## Các bước kiểm tra

### 1. Kiểm tra Backend Controller
File: `auth-service/src/main/java/com/example/auth_service/controller/ActivityLogController.java`

Đảm bảo có các endpoint:
```java
@DeleteMapping("/{id}")
@PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DELETE_ACTIVITY_LOG')")
public ApiResponse<String> deleteActivityLog(@PathVariable Long id) {
    // ...
}

@DeleteMapping("/bulk")
@PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DELETE_ACTIVITY_LOG')")
public ApiResponse<String> deleteActivityLogsBulk(@RequestBody List<Long> ids) {
    // ...
}
```

### 2. Restart Backend Service
```bash
# Dừng service hiện tại
# Sau đó start lại
cd auth-service
mvn spring-boot:run
# hoặc
./mvnw spring-boot:run
```

### 3. Kiểm tra Endpoint có hoạt động không
Sử dụng Postman hoặc curl để test:

```bash
# Test delete single log (thay {id} bằng ID thực tế)
curl -X DELETE http://localhost:8080/api/activity-logs/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Test bulk delete
curl -X DELETE http://localhost:8080/api/activity-logs/bulk \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[1, 2, 3]"
```

### 4. Kiểm tra Logs Backend
Xem logs của Spring Boot để tìm lỗi:
- Kiểm tra xem endpoint có được register không
- Kiểm tra xem có lỗi permission không
- Kiểm tra xem có lỗi routing không

### 5. Kiểm tra Permission
Đảm bảo user có role ADMIN hoặc permission `DELETE_ACTIVITY_LOG`:
- Kiểm tra trong database
- Kiểm tra trong JWT token

## Giải pháp tạm thời
Nếu không thể sửa backend ngay, frontend đã có fallback mechanism:
- Tự động thử xóa từng cái một nếu bulk delete fail
- Hiển thị thông báo lỗi rõ ràng

## Sau khi sửa
1. Restart backend
2. Test lại endpoint bằng Postman/curl
3. Test trên frontend
4. Kiểm tra logs để đảm bảo không còn lỗi

