package com.pos.system.scheduled;

import com.pos.system.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertScheduler {

    private final AlertService alertService;

    /**
     * Ejecuta cada hora en horario laboral (8am - 6pm)
     * Revisa productos bajo stock mínimo y genera alertas
     */
    @Scheduled(cron = "0 0 8-18 * * MON-FRI")
    public void checkLowStock() {
        log.info("Iniciando revisión programada de stock bajo...");
        int alertsGenerated = alertService.generateLowStockAlerts();
        log.info("Revisión completada. Alertas generadas: {}", alertsGenerated);
    }

    /**
     * Ejecuta una vez al día (7:00 AM) para limpiar alertas viejas
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void cleanupOldAlerts() {
        log.info("Iniciando limpieza de alertas antiguas...");
        // Las alertas resueltas de más de 30 días pueden archivarse
        // Por ahora solo log
        log.info("Limpieza completada.");
    }
}