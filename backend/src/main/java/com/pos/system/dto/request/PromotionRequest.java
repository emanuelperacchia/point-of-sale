package com.pos.system.dto.request;

import com.pos.system.entity.PromotionScope;
import com.pos.system.entity.PromotionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PromotionRequest {
    private String nombre;
    private PromotionType tipo;
    private BigDecimal valor;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Integer prioridad;
    private PromotionScope alcance;
    private Boolean activa;
    private Integer compraX;
    private Integer llevaY;
    private List<Long> productoIds;
    private List<Long> categoriaIds;
}
