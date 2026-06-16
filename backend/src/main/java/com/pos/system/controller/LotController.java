package com.pos.system.controller;

import com.pos.system.dto.response.LoteTraceabilityResponse;
import com.pos.system.entity.LoteProduccion;
import com.pos.system.entity.Product;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.LoteProduccionRepository;
import com.pos.system.repository.ProductionOrderComponentRepository;
import com.pos.system.repository.ProductionOrderRepository;
import com.pos.system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class LotController {

    private final LoteProduccionRepository loteProduccionRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductionOrderComponentRepository poComponentRepository;
    private final ProductRepository productRepository;

    @GetMapping("/{loteId}/traceability")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','INVENTARIO')")
    public ResponseEntity<LoteTraceabilityResponse> getTraceability(@PathVariable String loteId) {
        // Support both numeric ID and lot number
        LoteProduccion lote;
        try {
            lote = loteProduccionRepository.findById(Long.parseLong(loteId))
                    .orElse(null);
        } catch (NumberFormatException e) {
            lote = loteProduccionRepository.findByNumeroLote(loteId)
                    .orElse(null);
        }

        if (lote == null) {
            lote = loteProduccionRepository.findByNumeroLote(loteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
        }

        var components = poComponentRepository.findByProductionOrderId(lote.getProductionOrderId());

        Product pt = productRepository.findById(lote.getProductoTerminadoId()).orElse(null);

        List<LoteTraceabilityResponse.MateriaPrimaUsada> mps = components.stream().map(poc -> {
            // In a real system, this would trace back to purchase lots
            // For now, we show the raw materials used
            return LoteTraceabilityResponse.MateriaPrimaUsada.builder()
                    .productoNombre("Componente #" + poc.getBomComponentId())
                    .cantidad(poc.getCantidadConsumida() != null
                            ? poc.getCantidadConsumida().add(poc.getMermaReal() != null ? poc.getMermaReal() : java.math.BigDecimal.ZERO)
                            : java.math.BigDecimal.ZERO)
                    .loteCompra("N/D")
                    .proveedor("N/D")
                    .build();
        }).toList();

        return ResponseEntity.ok(LoteTraceabilityResponse.builder()
                .numeroLote(lote.getNumeroLote())
                .fechaProduccion(lote.getFechaProduccion())
                .cantidad(lote.getCantidad())
                .productoTerminadoNombre(pt != null ? pt.getName() : null)
                .productionOrderId(lote.getProductionOrderId())
                .materiasPrimas(mps)
                .build());
    }
}
