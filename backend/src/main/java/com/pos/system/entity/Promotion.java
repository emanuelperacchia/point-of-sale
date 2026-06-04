package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionType tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fechaDesde;

    @Column(nullable = false)
    private LocalDate fechaHasta;

    @Column(nullable = false)
    private Integer prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionScope alcance;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    // Para COMPRA_X_LLEVA_Y: cuántas unidades comprar
    @Column
    private Integer compraX;

    // Para COMPRA_X_LLEVA_Y: cuántas unidades llevar (gratis o con descuento)
    @Column
    private Integer llevaY;

    // Productos específicos a los que aplica
    @ElementCollection
    @CollectionTable(name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "product_id")
    @Builder.Default
    private List<Long> productoIds = new ArrayList<>();

    // Categorías a las que aplica
    @ElementCollection
    @CollectionTable(name = "promotion_categories",
            joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "category_id")
    @Builder.Default
    private List<Long> categoriaIds = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
