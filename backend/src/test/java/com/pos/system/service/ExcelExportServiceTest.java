package com.pos.system.service;

import com.pos.system.service.ExcelExportService.RowWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    private ExcelExportService excelExportService;

    @BeforeEach
    void setUp() {
        excelExportService = new ExcelExportService();
    }

    @Test
    void generate_WithData_ShouldReturnValidWorkbook() throws Exception {
        // Given
        List<String> headers = List.of("Fecha", "Tipo", "Total");
        List<String[]> data = List.of(
                new String[]{"2026-01-05", "FACTURA_B", "1210.00"},
                new String[]{"2026-01-10", "FACTURA_A", "5000.00"}
        );

        RowWriter<String[]> writer = (row, index, item) -> {
            ExcelExportService.cellString(row, 0, item[0]);
            ExcelExportService.cellString(row, 1, item[1]);
            ExcelExportService.cellNumeric(row, 2, Double.parseDouble(item[2]));
        };

        // When
        byte[] bytes = excelExportService.generate("Test Sheet", headers, data, writer);

        // Then
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Read back and verify
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Test Sheet", sheet.getSheetName());

            // Header row
            Row headerRow = sheet.getRow(0);
            assertEquals("Fecha", headerRow.getCell(0).getStringCellValue());
            assertEquals("Tipo", headerRow.getCell(1).getStringCellValue());
            assertEquals("Total", headerRow.getCell(2).getStringCellValue());

            // Data rows
            assertEquals("2026-01-05", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("FACTURA_B", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(1210.00, sheet.getRow(1).getCell(2).getNumericCellValue(), 0.001);

            assertEquals("2026-01-10", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }

    @Test
    void generate_WithFooterCallback_ShouldIncludeFooterRow() throws Exception {
        // Given
        List<String> headers = List.of("Concepto", "Monto");
        List<String[]> data = List.of(
                new String[]{"Item 1", "100"},
                new String[]{"Item 2", "200"}
        );

        RowWriter<String[]> writer = (row, index, item) -> {
            ExcelExportService.cellString(row, 0, item[0]);
            ExcelExportService.cellNumeric(row, 1, Double.parseDouble(item[1]));
        };

        Consumer<Workbook> footer = wb -> {
            Sheet sheet = wb.getSheetAt(0);
            Row footerRow = sheet.createRow(data.size() + 1); // row index 3 (0 header + 2 data + 1 blank = 3)
            ExcelExportService.cellString(footerRow, 0, "TOTAL");
            ExcelExportService.cellNumeric(footerRow, 1, 300);
        };

        // When
        byte[] bytes = excelExportService.generate("Footer Test", headers, data, writer, footer);

        // Then
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row footerRow = sheet.getRow(3);
            assertNotNull(footerRow);
            assertEquals("TOTAL", footerRow.getCell(0).getStringCellValue());
            assertEquals(300, footerRow.getCell(1).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void generate_WithEmptyData_ShouldReturnOnlyHeader() throws Exception {
        // Given
        List<String> headers = List.of("Col1", "Col2");
        List<String> data = List.of();

        RowWriter<String> writer = (row, index, item) -> {
            ExcelExportService.cellString(row, 0, item);
        };

        // When
        byte[] bytes = excelExportService.generate("Empty", headers, data, writer);

        // Then
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet.getRow(0)); // header exists
            assertNull(sheet.getRow(1));    // no data rows
        }
    }

    @Test
    void generate_WithoutFooter_ShouldStillWork() throws Exception {
        List<String> headers = List.of("A");
        List<String> data = List.of("value");

        RowWriter<String> writer = (row, index, item) ->
                ExcelExportService.cellString(row, 0, item);

        byte[] bytes = excelExportService.generate("No Footer", headers, data, writer);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void cellString_ShouldCreateStringCell() {
        Row row = new XSSFWorkbook().createSheet().createRow(0);
        ExcelExportService.cellString(row, 0, "hello");

        assertEquals("hello", row.getCell(0).getStringCellValue());
        assertEquals(CellType.STRING, row.getCell(0).getCellType());
    }

    @Test
    void cellString_WithNull_ShouldCreateEmptyCell() {
        Row row = new XSSFWorkbook().createSheet().createRow(0);
        ExcelExportService.cellString(row, 0, null);

        assertEquals("", row.getCell(0).getStringCellValue());
    }

    @Test
    void cellNumeric_ShouldCreateNumericCell() {
        Row row = new XSSFWorkbook().createSheet().createRow(0);
        ExcelExportService.cellNumeric(row, 0, 123.45);

        assertEquals(123.45, row.getCell(0).getNumericCellValue(), 0.001);
        assertEquals(CellType.NUMERIC, row.getCell(0).getCellType());
    }

    @Test
    void cellNumeric_WithNull_ShouldCreateZeroCell() {
        Row row = new XSSFWorkbook().createSheet().createRow(0);
        ExcelExportService.cellNumeric(row, 0, null);

        assertEquals(0.0, row.getCell(0).getNumericCellValue(), 0.001);
    }

    @Test
    void safeSheetName_ShouldTruncateLongNames() throws Exception {
        // Can only test indirectly through generate
        String longName = "A".repeat(50);

        byte[] bytes = excelExportService.generate(
                longName,
                List.of("H"),
                List.of("data"),
                (row, idx, item) -> ExcelExportService.cellString(row, 0, item)
        );

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            String sheetName = workbook.getSheetName(0);
            assertTrue(sheetName.length() <= 31);
        }
    }

    @Test
    void safeSheetName_ShouldRemoveSpecialChars() throws Exception {
        String illegalName = "Sheet [Name]*?:/\\Test";

        byte[] bytes = excelExportService.generate(
                illegalName,
                List.of("H"),
                List.of("data"),
                (row, idx, item) -> ExcelExportService.cellString(row, 0, item)
        );

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            String sheetName = workbook.getSheetName(0);
            assertFalse(sheetName.contains("["));
            assertFalse(sheetName.contains("]"));
            assertFalse(sheetName.contains("*"));
            assertFalse(sheetName.contains(":"));
            assertFalse(sheetName.contains("/"));
            assertFalse(sheetName.contains("\\"));
        }
    }
}
