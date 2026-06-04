package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    private LocalDateTime lastAttemptTime;

    private LocalDateTime blockedUntil;

    public boolean isBlocked() {
        return blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementAttempts() {
        this.attemptCount++;
        this.lastAttemptTime = LocalDateTime.now();
    }

    public void reset() {
        this.attemptCount = 0;
        this.blockedUntil = null;
    }
}
