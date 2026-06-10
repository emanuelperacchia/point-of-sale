package com.pos.system.service;

import com.pos.system.dto.response.NotificationResponse;
import com.pos.system.entity.Notification;
import com.pos.system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse crear(Long userId, String titulo, String mensaje) {
        if (userId == null) {
            // Cannot notify if user is not linked
            return null;
        }
        Notification notification = Notification.builder()
                .userId(userId)
                .titulo(titulo)
                .mensaje(mensaje)
                .build();
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNoLeidas(Long userId) {
        return notificationRepository.findByUserIdAndLeidoFalseOrderByCreadoEnDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getTodas(Long userId) {
        return notificationRepository.findByUserIdOrderByCreadoEnDesc(userId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countNoLeidas(Long userId) {
        return notificationRepository.countByUserIdAndLeidoFalse(userId);
    }

    @Transactional
    public void marcarLeida(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setLeido(true);
            notificationRepository.save(n);
        });
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).userId(n.getUserId())
                .titulo(n.getTitulo()).mensaje(n.getMensaje())
                .leido(n.getLeido()).creadoEn(n.getCreadoEn())
                .build();
    }
}
