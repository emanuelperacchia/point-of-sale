package com.pos.system.entity.legal;

import com.pos.system.entity.business.Branch;
import com.pos.system.entity.system.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Branch business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String holdType;

    @Column(length = 500)
    private String reason;

    @Column(length = 1000)
    private String legalBasis;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    @Column(name = "data_deletion_date")
    private LocalDateTime dataDeletionDate;

    @Column(name = "legal_retention_days")
    @Builder.Default
    private Integer legalRetentionDays = 5;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(length = 200)
    private String resolvedBy;
}
