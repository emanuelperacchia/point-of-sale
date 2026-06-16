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
@Table(name = "lotes_produccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_order_id", nullable = false)
    private Long productionOrderId;

    @Column(name = "numero_lote", nullable = false, length = 30, unique = true)
    private String numeroLote;

    @Column(name = "fecha_produccion", nullable = false)
    private LocalDate fechaProduccion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "producto_terminado_id", nullable = false)
    private Long productoTerminadoId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
