package com.pos.system.controller;

import com.pos.system.entity.DeviceToken;
import com.pos.system.repository.DeviceTokenRepository;
import com.pos.system.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Dispositivos", description = "Registro de dispositivos para notificaciones push")
public class DeviceController {

    private final DeviceTokenRepository deviceTokenRepository;

    @PostMapping("/register")
    @Operation(summary = "Registrar token FCM para notificaciones push")
    public ResponseEntity<Void> registerDevice(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String fcmToken = body.get("fcmToken");
        String plataforma = body.getOrDefault("plataforma", "ANDROID");

        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Si ya existe, actualizar
        var existing = deviceTokenRepository.findByFcmToken(fcmToken);
        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setActivo(true);
            token.setUserId(userDetails.getId());
            deviceTokenRepository.save(token);
        } else {
            deviceTokenRepository.save(DeviceToken.builder()
                    .userId(userDetails.getId())
                    .fcmToken(fcmToken)
                    .plataforma(plataforma)
                    .activo(true)
                    .build());
        }

        return ResponseEntity.ok().build();
    }
}
