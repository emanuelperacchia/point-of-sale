package com.pos.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Administración del sistema")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminLogController {

    @Value("${logging.file.path:./logs}")
    private String logDir;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener logs del sistema",
              description = "Retorna los últimos logs del sistema filtrados por nivel y rango de fechas. Solo ADMIN.")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "100") int limit) {

        // Validar nivel
        Set<String> nivelesValidos = Set.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");
        if (nivel != null && !nivelesValidos.contains(nivel.toUpperCase())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Nivel inválido. Valores permitidos: " + nivelesValidos
            ));
        }

        List<String> entries = new ArrayList<>();
        Path logPath = Paths.get(logDir);

        if (!Files.exists(logPath)) {
            return ResponseEntity.ok(Map.of(
                    "entries", List.of(),
                    "total", 0,
                    "limit", limit,
                    "message", "El directorio de logs no existe"
            ));
        }

        try (Stream<Path> files = Files.list(logPath)) {
            List<Path> logFiles = files
                    .filter(f -> f.toString().endsWith(".json") || f.toString().endsWith(".json.gz"))
                    .sorted((a, b) -> {
                        // Orden inverso (más reciente primero)
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return a.getFileName().toString().compareTo(b.getFileName().toString());
                        }
                    })
                    .toList();

            for (Path file : logFiles) {
                try {
                    List<String> fileLines;
                    if (file.toString().endsWith(".gz")) {
                        // Leer archivo comprimido
                        try (var gis = new java.util.zip.GZIPInputStream(
                                Files.newInputStream(file))) {
                            String content = new String(gis.readAllBytes(), StandardCharsets.UTF_8);
                            fileLines = Arrays.asList(content.split("\\n"));
                        }
                    } else {
                        fileLines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    }

                    for (String line : fileLines) {
                        if (line.isBlank()) continue;

                        // Filtrar por nivel si se especificó
                        if (nivel != null && !line.contains("\"level\":\"" + nivel.toUpperCase() + "\"")) {
                            continue;
                        }

                        entries.add(line);

                        if (entries.size() >= limit) break;
                    }
                } catch (IOException e) {
                    log.warn("No se pudo leer el archivo de log: {}", file);
                }

                if (entries.size() >= limit) break;
            }
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of(
                    "entries", List.of(),
                    "total", 0,
                    "limit", limit,
                    "message", "Error al leer directorio de logs: " + e.getMessage()
            ));
        }

        // Limitar resultados
        if (entries.size() > limit) {
            entries = entries.subList(0, limit);
        }

        return ResponseEntity.ok(Map.of(
                "entries", entries,
                "total", entries.size(),
                "limit", limit,
                "nivel", nivel != null ? nivel.toUpperCase() : "ALL"
        ));
    }
}
