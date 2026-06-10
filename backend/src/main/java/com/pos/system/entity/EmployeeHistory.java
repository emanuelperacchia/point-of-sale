package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeHistory {

    public enum Campo {
        CARGO, SALARIO, DEPARTAMENTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Campo campo;

    @Column(name = "valor_anterior", nullable = false, length = 255)
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, length = 255)
    private String valorNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "modificado_por")
    private Long modificadoPor;
}
