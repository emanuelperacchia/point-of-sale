package com.pos.system.repository;

import com.pos.system.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ipAddress")
    LoginAttempt findByIpAddress(@Param("ipAddress") String ipAddress);

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.lastAttemptTime < :threshold")
    void deleteOldAttempts(@Param("threshold") LocalDateTime threshold);
}
