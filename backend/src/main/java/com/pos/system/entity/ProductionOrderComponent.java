package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "production_order_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderComponent {

    public enum MotivoMerma {
        VENCIMIENTO, PROCESO, DEFECTO, OTRO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_order_id", nullable = false)
    private Long productionOrderId;

    @Column(name = "bom_component_id", nullable = false)
    private Long bomComponentId;

    @Column(name = "cantidad_planificada", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadPlanificada;

    @Column(name = "cantidad_consumida", precision = 12, scale = 4)
    private BigDecimal cantidadConsumida;

    @Column(name = "merma_real", precision = 12, scale = 4)
    private BigDecimal mermaReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_merma", length = 20)
    private MotivoMerma motivoMerma;
}
