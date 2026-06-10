package com.pos.system.repository;

import com.pos.system.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreadoEnDesc(Long userId);

    List<Notification> findByUserIdAndLeidoFalseOrderByCreadoEnDesc(Long userId);

    long countByUserIdAndLeidoFalse(Long userId);
}
