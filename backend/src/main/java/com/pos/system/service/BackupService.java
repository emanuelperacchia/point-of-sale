package com.pos.system.service;

import com.pos.system.entity.SystemBackup;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.SystemBackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final SystemBackupRepository systemBackupRepository;
    private final NotificationService notificationService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username:pos_user}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${file-storage.upload-dir:./backups}")
    private String backupDir;

    /**
     * Backup programado diario a las 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledBackup() {
        log.info("Iniciando backup programado...");
        try {
            ejecutarBackup("SCHEDULED", null);
            log.info("Backup programado completado exitosamente");
        } catch (Exception e) {
            log.error("Error en backup programado: {}", e.getMessage());
            notificationService.crear(null, "Error de Backup",
                    "El backup automático falló: " + e.getMessage());
        }
    }

    @Transactional
    public SystemBackup ejecutarBackup(String tipo, Long userId) {
        try {
            Files.createDirectories(Paths.get(backupDir));

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "pos_backup_" + timestamp + ".sql.gz";
            Path filePath = Paths.get(backupDir, filename);

            // Extraer nombre de base de datos de la URL JDBC
            String dbName = extractDbName(dbUrl);

            // Ejecutar pg_dump
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "--no-owner",
                    "--no-acl",
                    "-h", "localhost",
                    "-U", dbUser,
                    "-d", dbName
            );

            // Usar pipe para comprimir con GZip directamente
            try (OutputStream fos = new FileOutputStream(filePath.toFile());
                 GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

                pb.environment().put("PGPASSWORD", dbPassword != null ? dbPassword : "");
                Process process = pb.start();

                // Leer stdout y comprimir
                InputStream processOut = process.getInputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = processOut.read(buffer)) != -1) {
                    gzos.write(buffer, 0, len);
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    // Leer stderr para diagnóstico
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()));
                    StringBuilder errorMsg = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorMsg.append(line).append("\n");
                    }
                    throw new RuntimeException("pg_dump falló: " + errorMsg);
                }
            }

            long fileSize = Files.size(filePath);

            SystemBackup backup = systemBackupRepository.save(SystemBackup.builder()
                    .filename(filename)
                    .fileSize(fileSize)
                    .ubicacion(filePath.toAbsolutePath().toString())
                    .tipoStorage("LOCAL")
                    .creadoPor(userId)
                    .build());

            log.info("Backup creado: {} ({} bytes)", filename, fileSize);
            return backup;

        } catch (Exception e) {
            log.error("Error creando backup: {}", e.getMessage());
            throw new BadRequestException("Error al crear backup: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SystemBackup> listarBackups() {
        return systemBackupRepository.findAllByOrderByCreatedAtDesc();
    }

    private String extractDbName(String url) {
        // jdbc:postgresql://localhost:5432/pos_db -> pos_db
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0) {
            String afterSlash = url.substring(lastSlash + 1);
            int questionMark = afterSlash.indexOf('?');
            return questionMark >= 0 ? afterSlash.substring(0, questionMark) : afterSlash;
        }
        return "pos_db";
    }
}
