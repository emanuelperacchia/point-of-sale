package com.pos.system.repository;

import com.pos.system.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserIdAndActivoTrue(Long userId);

    Optional<DeviceToken> findByFcmToken(String fcmToken);
}
