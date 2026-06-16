package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "bom_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "componente_id", nullable = false)
    private Long componenteId;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "es_merma_esperada")
    @Builder.Default
    private Boolean esMermaEsperada = false;

    @Column(name = "porcentaje_merma_esperado", precision = 5, scale = 2)
    private BigDecimal porcentajeMermaEsperado;
}
