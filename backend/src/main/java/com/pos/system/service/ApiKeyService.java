package com.pos.system.service;

import com.pos.system.dto.request.ApiKeyRequest;
import com.pos.system.dto.response.ApiKeyResponse;
import com.pos.system.entity.ApiKey;
import com.pos.system.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public ApiKeyResponse createApiKey(ApiKeyRequest request) {
        ApiKey apiKey = new ApiKey();
        apiKey.setNombre(request.getNombre());
        apiKey.setDescripcion(request.getDescripcion());
        apiKey.setActivo(request.getActivo());
        apiKey.setRateLimit(request.getRateLimit());
        apiKey.setExpiracion(request.getExpiracion());
        
        ApiKey saved = apiKeyRepository.save(apiKey);
        return mapToResponse(saved);
    }

    public ApiKeyResponse getApiKeyById(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada con ID: " + id));
        return mapToResponse(apiKey);
    }

    public List<ApiKeyResponse> getAllApiKeys() {
        return apiKeyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ApiKeyResponse updateApiKey(Long id, ApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada con ID: " + id));
        
        apiKey.setNombre(request.getNombre());
        apiKey.setDescripcion(request.getDescripcion());
        apiKey.setRateLimit(request.getRateLimit());
        apiKey.setExpiracion(request.getExpiracion());
        
        ApiKey saved = apiKeyRepository.save(apiKey);
        return mapToResponse(saved);
    }

    public void deleteApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API Key no encontrada con ID: " + id));
        apiKeyRepository.delete(apiKey);
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setNombre(apiKey.getNombre());
        response.setDescripcion(apiKey.getDescripcion());
        response.setRateLimit(apiKey.getRateLimit());
        response.setExpiracion(apiKey.getExpiracion());
        response.setActivo(apiKey.isActivo());
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setUpdatedAt(apiKey.getUpdatedAt());
        return response;
    }
}
