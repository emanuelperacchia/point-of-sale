package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(nullable = false, length = 500)
    private String configValue;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String dataType;

    @Column(length = 500)
    private String validationRules;

    @Column(length = 500)
    private String defaultValue;

    @Column(length = 50)
    @Builder.Default
    private String groupName = "GENERAL";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper methods for type conversion
    public Integer getIntegerValue() {
        return Integer.parseInt(configValue);
    }

    public Double getDoubleValue() {
        return Double.parseDouble(configValue);
    }

    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(configValue);
    }

    public Long getLongValue() {
        return Long.parseLong(configValue);
    }
}