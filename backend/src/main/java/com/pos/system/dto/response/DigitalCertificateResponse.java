package com.pos.system.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Respuesta de certificado digital — NUNCA expone el binario del keystore
 * ni la contraseña (ni cifrada).
 */
@Builder
public record DigitalCertificateResponse(
        Long id,
        String alias,
        String storeType,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        String issuer,
        String subject,
        String serialNumber,
        Boolean active,
        LocalDateTime createdAt
) {
}