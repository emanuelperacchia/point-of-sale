package com.pos.system.entity.product;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProductTipo tipo = ProductTipo.PRODUCTO_TERMINADO;

    @Column(name = "costo_produccion", precision = 12, scale = 2)
    private BigDecimal costoProduccion;

    @Column(name = "stock_reservado", nullable = false)
    @Builder.Default
    private Integer stockReservado = 0;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ProductTipo {
        PRODUCTO_TERMINADO, SERVICIO, MATERIA_PRIMA
    }
}
