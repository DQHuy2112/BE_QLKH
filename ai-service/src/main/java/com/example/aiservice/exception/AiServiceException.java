package com.example.aiservice.exception;

public class AiServiceException extends RuntimeException {
    private final boolean isQuotaExceeded;
    private final boolean isTemporary;

    public AiServiceException(String message) {
        super(message);
        this.isQuotaExceeded = false;
        this.isTemporary = false;
    }

    public AiServiceException(String message, boolean isQuotaExceeded, boolean isTemporary) {
        super(message);
        this.isQuotaExceeded = isQuotaExceeded;
        this.isTemporary = isTemporary;
    }

    public boolean isQuotaExceeded() {
        return isQuotaExceeded;
    }

    public boolean isTemporary() {
        return isTemporary;
    }
}

