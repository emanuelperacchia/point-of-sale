package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_change_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftChangeRequest {

    public enum Estado {
        PENDIENTE, APROBADO, RECHAZADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "fecha_original", nullable = false)
    private LocalDate fechaOriginal;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "revisado_por")
    private Long revisadoPor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
