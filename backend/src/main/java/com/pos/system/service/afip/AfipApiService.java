package com.pos.system.service.afip;

import com.pos.system.entity.afip.AfipTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AfipApiService {

    public String getTicketAccess(Long userId) {
        // Simplificado - en producción se obtendría de la BD
        return "TA-" + System.currentTimeMillis();
    }
}
