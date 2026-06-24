package com.pos.system.service;

import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkExportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    private final Path exportDir = Paths.get("./exports");
    private final Map<String, ExportJob> jobs = new ConcurrentHashMap<>();

    public static class ExportJob {
        public final String jobId;
        public volatile String status = "PROCESSING";
        public volatile String filename;
        public volatile String error;

        public ExportJob(String jobId) { this.jobId = jobId; }
    }

    /**
     * Exportación síncrona para datasets chicos (< 10.000 filas).
     */
    @Transactional(readOnly = true)
    public Path exportSync(String entidad, LocalDate desde, LocalDate hasta, String format) {
        try {
            Files.createDirectories(exportDir);
            String filename = entidad + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + "." + format;
            Path filePath = exportDir.resolve(filename);

            List<String[]> data = loadData(entidad, desde, hasta);

            if ("csv".equalsIgnoreCase(format)) {
                writeCsv(filePath, data);
            } else {
                writeExcel(filePath, data);
            }

            return filePath;
        } catch (Exception e) {
            throw new BadRequestException("Error en exportación: " + e.getMessage());
        }
    }

    /**
     * Exportación asíncrona para datasets grandes.
     */
    @Async
    public void exportAsync(String jobId, String entidad, LocalDate desde, LocalDate hasta,
                            String format, Long userId) {
        try {
            Files.createDirectories(exportDir);
            String filename = entidad + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + "." + format;
            Path filePath = exportDir.resolve(filename);

            List<String[]> data = loadData(entidad, desde, hasta);

            if ("csv".equalsIgnoreCase(format)) {
                writeCsv(filePath, data);
            } else {
                writeExcel(filePath, data);
            }

            ExportJob job = jobs.get(jobId);
            if (job != null) {
                job.status = "COMPLETED";
                job.filename = filename;
            }

            if (userId != null) {
                notificationService.crear(userId, "Exportación completada",
                        "La exportación de " + entidad + " está lista para descargar: " + filename);
            }
        } catch (Exception e) {
            log.error("Error en exportación asíncrona {}", jobId, e);
            ExportJob job = jobs.get(jobId);
            if (job != null) {
                job.status = "ERROR";
                job.error = e.getMessage();
            }
        }
    }

    public ExportJob createJob() {
        String jobId = UUID.randomUUID().toString();
        ExportJob job = new ExportJob(jobId);
        jobs.put(jobId, job);
        return job;
    }

    public ExportJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    private List<String[]> loadData(String entidad, LocalDate desde, LocalDate hasta) {
        return switch (entidad.toUpperCase()) {
            case "SALES" -> loadSales(desde, hasta);
            case "PRODUCTS" -> loadProducts();
            case "CLIENTS" -> loadClients();
            case "EMPLOYEES" -> loadEmployees();
            default -> throw new BadRequestException("Entidad no soportada: " + entidad);
        };
    }

    private List<String[]> loadSales(LocalDate desde, LocalDate hasta) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Cliente", "Total", "Fecha", "Estado"});
        var sales = desde != null && hasta != null
                ? saleRepository.findByCreatedAtBetween(desde.atStartOfDay(), hasta.atTime(23, 59, 59))
                : saleRepository.findAll();
        for (Sale s : sales) {
            rows.add(new String[]{
                    String.valueOf(s.getId()),
                    s.getClient() != null ? s.getClient().getName() : "N/A",
                    s.getTotal().toString(),
                    s.getCreatedAt() != null ? s.getCreatedAt().toString() : "",
                    s.getStatus().name()
            });
        }
        return rows;
    }

    private List<String[]> loadProducts() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Código", "Nombre", "Precio", "Activo"});
        for (Product p : productRepository.findAll()) {
            rows.add(new String[]{
                    String.valueOf(p.getId()),
                    p.getSku(),
                    p.getName(),
                    p.getPrice().toString(),
                    p.getActive() ? "Sí" : "No"
            });
        }
        return rows;
    }

    private List<String[]> loadClients() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Nombre", "Documento", "Email", "Teléfono"});
        for (Client c : clientRepository.findAll()) {
            rows.add(new String[]{
                    String.valueOf(c.getId()),
                    c.getName(),
                    c.getDocumentNumber(),
                    c.getEmail() != null ? c.getEmail() : "",
                    c.getPhone() != null ? c.getPhone() : ""
            });
        }
        return rows;
    }

    private List<String[]> loadEmployees() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Nombre", "Apellido", "DNI", "Cargo", "Departamento"});
        for (Employee e : employeeRepository.findByActivoTrue()) {
            rows.add(new String[]{
                    String.valueOf(e.getId()),
                    e.getNombre(),
                    e.getApellido(),
                    e.getDni(),
                    e.getCargo(),
                    e.getDepartamento()
            });
        }
        return rows;
    }

    private void writeCsv(Path path, List<String[]> data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String[] row : data) {
                writer.write(String.join(",", escapeCsv(row)));
                writer.newLine();
            }
        }
    }

    private String[] escapeCsv(String[] row) {
        String[] escaped = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            String val = row[i] != null ? row[i] : "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                val = "\"" + val.replace("\"", "\"\"") + "\"";
            }
            escaped[i] = val;
        }
        return escaped;
    }

    private void writeExcel(Path path, List<String[]> data) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Export");
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i);
                String[] values = data.get(i);
                for (int j = 0; j < values.length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(values[j] != null ? values[j] : "");
                    if (i == 0) cell.setCellStyle(headerStyle);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                workbook.write(fos);
            }
        }
    }
}
