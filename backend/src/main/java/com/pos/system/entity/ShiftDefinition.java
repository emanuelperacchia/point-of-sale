package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "shift_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "dias_semana", nullable = false)
    private int diasSemana; // bitmask: 1=lunes, 2=martes, 4=miercoles, 8=jueves, 16=viernes, 32=sabado, 64=domingo

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
