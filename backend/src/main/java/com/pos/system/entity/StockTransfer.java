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
@Table(name = "stock_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    public enum Estado {
        SOLICITADA,
        EN_TRANSITO,
        RECIBIDA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sucursal_origen_id", nullable = false)
    private Long sucursalOrigenId;

    @Column(name = "sucursal_destino_id", nullable = false)
    private Long sucursalDestinoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.SOLICITADA;

    @Column(length = 500)
    private String motivo;

    @Column(name = "solicitado_por")
    private Long solicitadoPor;

    @Column(name = "despachado_por")
    private Long despachadoPor;

    @Column(name = "recibido_por")
    private Long recibidoPor;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
