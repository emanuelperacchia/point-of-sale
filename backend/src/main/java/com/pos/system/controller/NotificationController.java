package com.pos.system.controller;

import com.pos.system.dto.response.NotificationResponse;
import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(notificationService.getTodas(userDetails.getUser().getId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> unread(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(notificationService.getNoLeidas(userDetails.getUser().getId()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        long count = notificationService.countNoLeidas(userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.marcarLeida(id);
        return ResponseEntity.ok().build();
    }
}
