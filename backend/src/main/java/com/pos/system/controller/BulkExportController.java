package com.pos.system.controller;

import com.pos.system.security.UserDetailsImpl;
import com.pos.system.service.BulkExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Exportación Masiva", description = "Exportación de datos en XLSX/CSV")
public class BulkExportController {

    private final BulkExportService bulkExportService;

    @GetMapping("/bulk")
    @Operation(summary = "Exportar datos",
               description = "Exporta datos de una entidad. Para datasets grandes (>10K filas) usar modo asíncrono.")
    public ResponseEntity<?> exportBulk(
            @RequestParam String entidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(defaultValue = "sync") String mode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if ("async".equalsIgnoreCase(mode)) {
            BulkExportService.ExportJob job = bulkExportService.createJob();
            bulkExportService.exportAsync(job.jobId, entidad, desde, hasta, format,
                    userDetails != null ? userDetails.getId() : null);
            return ResponseEntity.accepted().body(Map.of(
                    "jobId", job.jobId,
                    "status", "PROCESSING",
                    "message", "Exportación iniciada. Use GET /api/export/jobs/" + job.jobId + " para consultar estado"
            ));
        }

        try {
            Path filePath = bulkExportService.exportSync(entidad, desde, hasta, format);
            return downloadFile(filePath);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Consultar estado de exportación asíncrona")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        BulkExportService.ExportJob job = bulkExportService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "jobId", job.jobId,
                "status", job.status,
                "filename", job.filename != null ? job.filename : "",
                "error", job.error != null ? job.error : ""
        ));
    }

    @GetMapping("/jobs/{jobId}/download")
    @Operation(summary = "Descargar resultado de exportación asíncrona")
    public ResponseEntity<?> downloadJob(@PathVariable String jobId) {
        BulkExportService.ExportJob job = bulkExportService.getJob(jobId);
        if (job == null || !"COMPLETED".equals(job.status)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job no disponible o en proceso"));
        }

        try {
            Path filePath = Path.of("./exports", job.filename);
            return downloadFile(filePath);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<Resource> downloadFile(Path filePath) throws Exception {
        Resource resource = new InputStreamResource(new FileInputStream(filePath.toFile()));
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .body(resource);
    }
}
