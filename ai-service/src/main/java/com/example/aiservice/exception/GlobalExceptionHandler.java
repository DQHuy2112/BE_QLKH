package com.example.aiservice.exception;

import com.example.aiservice.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceException(AiServiceException ex) {
        // Trả về HTTP 200 với message thân thiện cho lỗi AI (quota, timeout, etc.)
        // Frontend sẽ xử lý và hiển thị toast message, không hiển thị error box
        String friendlyMessage = "Hiện tại hệ thống AI đang quá tải hoặc tạm thời không khả dụng. Vui lòng thử lại sau ít phút.";
        return ResponseEntity.ok(ApiResponse.fail(friendlyMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        // Kiểm tra nếu là lỗi liên quan đến AI (Gemini API)
        String message = ex.getMessage();
        if (message != null && (message.contains("Gemini") || message.contains("AI") || message.contains("quota") || message.contains("429"))) {
            // Trả về HTTP 200 với message thân thiện cho lỗi AI
            String friendlyMessage = "Hiện tại hệ thống AI đang quá tải hoặc tạm thời không khả dụng. Vui lòng thử lại sau ít phút.";
            return ResponseEntity.ok(ApiResponse.fail(friendlyMessage));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ex.getMessage()));
    }
}


