package com.pos.system.service;

import com.pos.system.dto.request.ApiKeyRequest;
import com.pos.system.dto.response.ApiKeyResponse;
import com.pos.system.entity.ApiKey;
import com.pos.system.entity.system.User;
import com.pos.system.repository.ApiKeyRepository;
import com.pos.system.repository.system.UserRepository;
import com.pos.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AfipAuthService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    public AfipResponse authenticate(AfipAuthRequest request) {
        AfipApiService client = getAfipApiService(request.getUserId());
        String ticket = client.obtainTicket(request.getRuc());

        AfipTicket ticket = AfipTicket.builder()
                .ta(request.getTicket())
                .fechaVencimiento(request.getFechaVencimiento())
                .build();
        return ticket;
    }

    public String getTicketAccess(Long userId) {
        AfipApiService client = getAfipApiService(userId);
        return client.getTicketAccess();
    }

    private AfipApiService getAfipApiService(Long userId) {
        // Simplificado - en producción se obtendría la configuración del usuario
        return new AfipApiService();
    }
}
