package com.pos.system.controller;

import com.pos.system.service.ecommerce.OrderImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/integrations/ecommerce")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "E-commerce Integration", description = "Integración con tienda online")
public class EcommerceIntegrationController {

    private final OrderImportService orderImportService;

    @GetMapping("/sync-status")
    @Operation(summary = "Estado de la sincronización", description = "Última sync, productos, pedidos, errores")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        // TODO: implementar con datos reales de repositorios
        return ResponseEntity.ok(Map.of(
                "ultimaSync", "N/A",
                "productosSincronizados", 0,
                "pedidosImportadosHoy", 0,
                "errores", 0
        ));
    }

    @PostMapping("/sync-now")
    @Operation(summary = "Forzar sincronización inmediata")
    public ResponseEntity<Map<String, String>> forceSync() {
        orderImportService.importPendingOrders();
        return ResponseEntity.ok(Map.of("mensaje", "Sincronización forzada completada"));
    }
}
