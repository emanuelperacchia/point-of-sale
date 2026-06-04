package com.pos.system.controller;

import com.pos.system.dto.request.PromotionRequest;
import com.pos.system.dto.response.PromotionResponse;
import com.pos.system.entity.Promotion;
import com.pos.system.repository.PromotionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(name = "Promociones", description = "CRUD de reglas de descuento y promociones")
@SecurityRequirement(name = "bearerAuth")
public class PromotionController {

    private final PromotionRepository promotionRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Listar todas las promociones")
    public ResponseEntity<List<PromotionResponse>> findAll() {
        List<PromotionResponse> list = promotionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO', 'VENDEDOR')")
    @Operation(summary = "Obtener promoción por ID")
    public ResponseEntity<PromotionResponse> findById(@PathVariable Long id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada: " + id));
        return ResponseEntity.ok(toResponse(promo));
    }

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Crear promoción")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        Promotion promo = mapToEntity(request);
        promo = promotionRepository.save(promo);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(promo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Actualizar promoción")
    public ResponseEntity<PromotionResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody PromotionRequest request) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada: " + id));

        existing.setNombre(request.getNombre());
        existing.setTipo(request.getTipo());
        existing.setValor(request.getValor());
        existing.setFechaDesde(request.getFechaDesde());
        existing.setFechaHasta(request.getFechaHasta());
        existing.setPrioridad(request.getPrioridad());
        existing.setAlcance(request.getAlcance());
        if (request.getActiva() != null) existing.setActiva(request.getActiva());
        existing.setCompraX(request.getCompraX());
        existing.setLlevaY(request.getLlevaY());
        if (request.getProductoIds() != null) existing.setProductoIds(request.getProductoIds());
        if (request.getCategoriaIds() != null) existing.setCategoriaIds(request.getCategoriaIds());

        existing = promotionRepository.save(existing);
        return ResponseEntity.ok(toResponse(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Eliminar promoción")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new EntityNotFoundException("Promoción no encontrada: " + id);
        }
        promotionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ──

    private PromotionResponse toResponse(Promotion p) {
        return PromotionResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .tipo(p.getTipo())
                .valor(p.getValor())
                .fechaDesde(p.getFechaDesde())
                .fechaHasta(p.getFechaHasta())
                .prioridad(p.getPrioridad())
                .alcance(p.getAlcance())
                .activa(p.getActiva())
                .compraX(p.getCompraX())
                .llevaY(p.getLlevaY())
                .productoIds(p.getProductoIds())
                .categoriaIds(p.getCategoriaIds())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private Promotion mapToEntity(PromotionRequest r) {
        Promotion p = new Promotion();
        p.setNombre(r.getNombre());
        p.setTipo(r.getTipo());
        p.setValor(r.getValor());
        p.setFechaDesde(r.getFechaDesde());
        p.setFechaHasta(r.getFechaHasta());
        p.setPrioridad(r.getPrioridad());
        p.setAlcance(r.getAlcance());
        p.setActiva(r.getActiva() != null ? r.getActiva() : true);
        p.setCompraX(r.getCompraX());
        p.setLlevaY(r.getLlevaY());
        p.setProductoIds(r.getProductoIds() != null ? r.getProductoIds() : List.of());
        p.setCategoriaIds(r.getCategoriaIds() != null ? r.getCategoriaIds() : List.of());
        return p;
    }
}
