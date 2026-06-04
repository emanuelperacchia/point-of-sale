package com.pos.system.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
public class HealthResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;
    private String version;

    public HealthResponse(String status, String message, LocalDateTime timestamp, String version) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.version = version;
    }

    public HealthResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "HealthResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", version='" + version + '\'' +
                '}';
    }
}