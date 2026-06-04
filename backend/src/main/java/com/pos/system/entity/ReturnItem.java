package com.pos.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Item de una devolución, vinculado al SaleItem original.
 */
@Entity
@Table(name = "return_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Column(name = "sale_item_id", nullable = false)
    private Long saleItemId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
}
