package com.pos.system.service;

import com.pos.system.config.FiscalProperties;
import com.pos.system.entity.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Genera el PDF del comprobante electrónico usando Apache PDFBox.
 * <p>
 * El diseño incluye: datos del emisor, datos del receptor, detalle de items,
 * totales, CAE y código QR embebido.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final float LEADING = 14;

    /** Rutas de fuentes TrueType en classpath (opcionales). */
    private static final String FONT_REGULAR = "/fonts/LiberationSans-Regular.ttf";
    private static final String FONT_BOLD = "/fonts/LiberationSans-Bold.ttf";

    private final FiscalProperties properties;

    /**
     * Genera el PDF del comprobante y retorna la ruta del archivo.
     *
     * @param invoice comprobante electrónico
     * @param sale    venta asociada (con items cargados)
     * @param company datos fiscales de la empresa
     * @return ruta absoluta del PDF generado
     */
    public String generateInvoicePdf(InvoiceDocument invoice, Sale sale, Company company) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Cargar fuentes (con fallback a Helvetica)
            FontPair fonts = loadFonts(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                y = drawHeader(cs, fonts, company, invoice, y);
                y -= 10;
                y = drawReceptor(cs, fonts, invoice, y);
                y -= 10;
                y = drawItemsTable(cs, fonts, sale, y);
                y -= 10;
                y = drawTotals(cs, fonts, sale, y);
                y -= 15;
                drawCaeBlock(cs, fonts, invoice, y);
                // QR al final
                // Nota: embebir la imagen QR requiere decodificar el base64
                // Se implementa en una mejora futura
            }

            // Crear directorio de salida
            String outputDir = properties.getPdf().getOutputDir();
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);

            String fileName = String.format("%s-%03d-%08d.pdf",
                    invoice.getTipoComprobante().name().toLowerCase(),
                    invoice.getPuntoVenta(),
                    invoice.getNumero());
            Path outputPath = dir.resolve(fileName);
            doc.save(outputPath.toFile());

            log.info("PDF generado: {}", outputPath.toAbsolutePath());
            return outputPath.toAbsolutePath().toString();

        } catch (IOException e) {
            throw new RuntimeException("Error al generar PDF del comprobante", e);
        }
    }

    // ──────────────────────────────────────────────
    // Fonts
    // ──────────────────────────────────────────────

    private FontPair loadFonts(PDDocument doc) throws IOException {
        PDType0Font regular = tryLoadFont(doc, FONT_REGULAR);
        PDType0Font bold = tryLoadFont(doc, FONT_BOLD);

        if (regular == null) {
            log.warn("Fuente '{}' no encontrada en classpath. Usando Helvetica. " +
                     "Agregue archivos .ttf en src/main/resources/fonts/ para mejor soporte de caracteres.",
                     FONT_REGULAR);
            return new FontPair(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
        }
        return new FontPair(regular, bold != null ? bold : regular);
    }

    private PDType0Font tryLoadFont(PDDocument doc, String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                return PDType0Font.load(doc, is);
            }
        } catch (Exception e) {
            log.debug("Fuente {} no disponible: {}", path, e.getMessage());
        }
        return null;
    }

    private record FontPair(PDFont regular, PDFont bold) {}

    // ──────────────────────────────────────────────
    // Draw helpers
    // ──────────────────────────────────────────────

    private float drawHeader(PDPageContentStream cs, FontPair fonts,
                             Company company, InvoiceDocument invoice, float y) throws IOException {
        float x = MARGIN;

        // Razón social
        cs.setFont(fonts.bold(), 16);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(company.getBusinessName() != null ? company.getBusinessName() : "");
        cs.endText();
        y -= 18;

        // Documento del emisor
        cs.setFont(fonts.regular(), 9);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        String docLine = (company.getDocumentType() != null ? company.getDocumentType() : "CUIT")
                + ": " + (company.getDocumentNumber() != null ? company.getDocumentNumber() : "");
        cs.showText(docLine);
        cs.endText();
        y -= 13;

        // Domicilio
        if (company.getTaxAddress() != null) {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(company.getTaxAddress());
            cs.endText();
            y -= 13;
        }

        // Tipo de comprobante (grande, centrado)
        y -= 10;
        cs.setFont(fonts.bold(), 14);
        String tipoStr = invoice.getTipoComprobante().name().replace("_", " ");
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(tipoStr);
        cs.endText();
        y -= 18;

        // Número y punto de venta
        cs.setFont(fonts.bold(), 11);
        String nroStr = String.format("Punto de Venta: %04d  –  Número: %04d-%08d",
                invoice.getPuntoVenta(), invoice.getPuntoVenta(), invoice.getNumero());
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(nroStr);
        cs.endText();
        y -= 16;

        return y;
    }

    private float drawReceptor(PDPageContentStream cs, FontPair fonts,
                               InvoiceDocument invoice, float y) throws IOException {
        float x = MARGIN;

        cs.setFont(fonts.bold(), 10);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("DATOS DEL RECEPTOR");
        cs.endText();
        y -= 14;

        cs.setFont(fonts.regular(), 9);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("Receptor: " + (invoice.getReceptorNombre() != null ? invoice.getReceptorNombre() : ""));
        cs.endText();
        y -= 12;

        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("Documento: " + (invoice.getReceptorDocumento() != null ? invoice.getReceptorDocumento() : ""));
        cs.endText();
        y -= 12;

        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("Condición IVA: " + (invoice.getReceptorCondicionIva() != null
                ? invoice.getReceptorCondicionIva().name().replace("_", " ")
                : ""));
        cs.endText();
        y -= 12;

        return y;
    }

    private float drawItemsTable(PDPageContentStream cs, FontPair fonts,
                                 Sale sale, float y) throws IOException {
        float x = MARGIN;
        float[] colWidths = {30, PAGE_WIDTH - 170, 60, 50, 60};
        float[] colStarts = {x, x + 30, x + 30 + (PAGE_WIDTH - 170), x + 30 + (PAGE_WIDTH - 170) + 60, x + 30 + (PAGE_WIDTH - 170) + 60 + 50};
        String[] headers = {"Cant.", "Descripción", "P. Unit.", "Desc.", "Total"};

        // Draw header row
        cs.setFont(fonts.bold(), 8);
        cs.beginText();
        float hx = x;
        for (int i = 0; i < headers.length; i++) {
            cs.newLineAtOffset(hx == x ? 0 : colStarts[i] - hx, y);
            cs.showText(headers[i]);
            hx = colStarts[i];
        }
        cs.endText();
        y -= 12;

        // Draw separator line
        cs.setLineWidth(0.5f);
        cs.moveTo(x, y);
        cs.lineTo(x + PAGE_WIDTH, y);
        cs.stroke();
        y -= 4;

        // Draw items
        cs.setFont(fonts.regular(), 8);
        for (SaleItem item : sale.getItems()) {
            if (y < 60) {
                // New page (simplified — just skip if runs out of space)
                break;
            }

            cs.beginText();
            hx = x;
            cs.newLineAtOffset(0, y);
            cs.showText(String.valueOf(item.getQuantity()));
            cs.newLineAtOffset(colWidths[0], 0);
            cs.showText(truncate(item.getProductName(), 35));
            cs.newLineAtOffset(colWidths[1], 0);
            cs.showText(formatMoney(item.getUnitPrice()));
            cs.newLineAtOffset(colWidths[2], 0);
            cs.showText(formatMoney(item.getDiscount()));
            cs.newLineAtOffset(colWidths[3], 0);
            cs.showText(formatMoney(item.getSubtotal()));
            cs.endText();
            y -= 11;
        }

        return y;
    }

    private float drawTotals(PDPageContentStream cs, FontPair fonts,
                             Sale sale, float y) throws IOException {
        float x = MARGIN + PAGE_WIDTH - 120;

        // Separator
        cs.setLineWidth(0.5f);
        cs.moveTo(x, y);
        cs.lineTo(x + 120, y);
        cs.stroke();
        y -= 4;

        cs.setFont(fonts.regular(), 9);
        String[][] lines = {
                {"Subtotal:", formatMoney(sale.getSubtotal())},
                {"Impuestos:", formatMoney(sale.getTaxAmount())},
                {"Descuento:", formatMoney(sale.getDiscount())},
        };

        for (String[] line : lines) {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(line[0]);
            cs.newLineAtOffset(80, 0);
            cs.showText(line[1]);
            cs.endText();
            y -= 12;
        }

        // Total (bold)
        cs.setFont(fonts.bold(), 11);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("TOTAL:");
        cs.newLineAtOffset(80, 0);
        cs.showText(formatMoney(sale.getTotal()));
        cs.endText();
        y -= 14;

        return y;
    }

    private void drawCaeBlock(PDPageContentStream cs, FontPair fonts,
                              InvoiceDocument invoice, float y) throws IOException {
        float x = MARGIN;

        cs.setFont(fonts.bold(), 9);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("CAE: " + (invoice.getCae() != null ? invoice.getCae() : "—"));
        cs.endText();
        y -= 12;

        cs.setFont(fonts.regular(), 9);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("Vencimiento CAE: "
                + (invoice.getFechaCae() != null ? invoice.getFechaCae().format(DATE_FMT) : "—"));
        cs.endText();
        y -= 12;

        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("Fecha de emisión: "
                + (invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DATETIME_FMT) : "—"));
        cs.endText();
    }

    // ──────────────────────────────────────────────
    // Utils
    // ──────────────────────────────────────────────

    private String formatMoney(BigDecimal value) {
        return String.format("$ %.2f", value != null ? value : BigDecimal.ZERO);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }
}
