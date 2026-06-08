package com.pos.system.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Servicio genérico para exportar datos a Excel (.xlsx).
 * <p>
 * Uso:
 * <pre>{@code
 * byte[] excel = excelExportService.generate(
 *     "Libro de Ventas",
 *     List.of("Fecha", "Tipo", "Número", "Total"),
 *     rows,
 *     (row, data) -> { /* mapear data a celdas *\/ },
 *     workbook -> { /* agregar totales al pie *\/ }
 * );
 * }</pre>
 */
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    /**
     * Genera un workbook .xlsx con header, filas de datos y un callback opcional para totales.
     *
     * @param sheetName   nombre de la hoja
     * @param headers     títulos de columna
     * @param data        datos a exportar
     * @param rowMapper   callback que escribe cada fila (índice 0 = primera fila de datos)
     * @param footerCallback callback opcional para escribir filas de totales al pie
     * @param <T>         tipo de los datos
     * @return bytes del archivo .xlsx
     */
    public <T> byte[] generate(
            String sheetName,
            List<String> headers,
            List<T> data,
            RowWriter<T> rowMapper,
            Consumer<Workbook> footerCallback) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(safeSheetName(sheetName));

            // Estilo del encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Fila de encabezado
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                rowMapper.write(row, i, data.get(i));
            }

            // Footer (totales)
            if (footerCallback != null) {
                footerCallback.accept(workbook);
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }

    /**
     * Versión simplificada sin footer.
     */
    public <T> byte[] generate(
            String sheetName,
            List<String> headers,
            List<T> data,
            RowWriter<T> rowMapper) {
        return generate(sheetName, headers, data, rowMapper, null);
    }

    /**
     * Crea una celda con valor string.
     */
    public static void cellString(Row row, int index, String value) {
        row.createCell(index, CellType.STRING).setCellValue(value != null ? value : "");
    }

    /**
     * Crea una celda con valor numérico.
     */
    public static void cellNumeric(Row row, int index, Number value) {
        row.createCell(index, CellType.NUMERIC)
                .setCellValue(value != null ? value.doubleValue() : 0.0);
    }

    private static String safeSheetName(String name) {
        if (name == null || name.isEmpty()) return "Sheet1";
        // Excel limita a 31 caracteres y no permite ciertos caracteres
        String safe = name.replaceAll("[\\[\\]*?:/\\\\]", "");
        return safe.length() > 31 ? safe.substring(0, 31) : safe;
    }

    @FunctionalInterface
    public interface RowWriter<T> {
        void write(Row row, int index, T item);
    }
}
