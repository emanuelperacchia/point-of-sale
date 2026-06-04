package com.pos.system.service;

import com.pos.system.dto.response.HealthResponse;

public interface HealthService {
    HealthResponse getSystemHealth();
    boolean checkDatabaseConnection();
    HealthResponse getSystemMetrics();
}
