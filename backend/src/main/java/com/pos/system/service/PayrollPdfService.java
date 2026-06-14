package com.pos.system.service;

import com.pos.system.entity.Payroll;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el recibo de sueldo en PDF usando Apache PDFBox.
 * Template propio de recibo de haberes — NO hereda del template fiscal.
 */
@Service
@RequiredArgsConstructor
public class PayrollPdfService {

    private static final int MARGIN = 50;
    private static final int PAGE_WIDTH = 595;
    private static final int ROW_HEIGHT = 16;

    private PDFont font;
    private PDFont fontBold;

    public byte[] generateReceipt(Payroll payroll, String nombreEmpleado, String cuil) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            font = loadFont(doc);
            fontBold = loadFontBold(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                int y = 750;

                // ── Header ──────────────────────────────────────────
                cs.setFont(fontBold, 14);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("RECIBO DE SUELDO");
                cs.endText();

                y -= 8;
                cs.setFont(font, 8);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Liquidación mensual - Período " + payroll.getMes() + "/" + payroll.getAnio());
                cs.endText();

                y -= 20;
                drawSeparator(cs, y);
                y -= 16;

                // ── Employee data ───────────────────────────────────
                cs.setFont(font, 10);
                String[] empLines = {
                        "Empleado: " + nombreEmpleado,
                        "CUIL: " + (cuil != null ? cuil : "-"),
                        "Fecha de emisión: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "AR")))
                };
                for (String line : empLines) {
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 14;
                }

                y -= 8;
                drawSeparator(cs, y);
                y -= 20;

                // ── Header column ───────────────────────────────────
                cs.setFont(fontBold, 9);
                drawRow(cs, y, "CONCEPTO", "IMPORTE");
                y -= ROW_HEIGHT;

                drawSeparator(cs, y);
                y -= 12;

                // ── Haberes ─────────────────────────────────────────
                cs.setFont(fontBold, 9);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("HABERES");
                cs.endText();
                y -= 6;

                cs.setFont(font, 9);
                y = drawConceptRow(cs, y, "Sueldo básico", payroll.getSueldoBasico());
                y = drawConceptRow(cs, y, "Plus horas extra", payroll.getPlusHorasExtra());
                y = drawConceptRow(cs, y, "Comisiones", payroll.getComisiones());
                y = drawConceptRow(cs, y, "Bono desempeño", payroll.getBonoDesempeno());

                drawSeparator(cs, y);
                y -= 8;
                cs.setFont(fontBold, 9);
                y = drawConceptRow(cs, y, "TOTAL HABERES", payroll.getTotalHaberes());

                y -= 14;
                drawSeparator(cs, y);
                y -= 12;

                // ── Descuentos ──────────────────────────────────────
                cs.setFont(fontBold, 9);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("DESCUENTOS");
                cs.endText();
                y -= 6;

                cs.setFont(font, 9);
                y = drawConceptRow(cs, y, "Jubilación (11%)", payroll.getDescJubilacion());
                y = drawConceptRow(cs, y, "Obra Social (3%)", payroll.getDescObraSocial());
                y = drawConceptRow(cs, y, "ANSES (3%)", payroll.getDescAnses());
                y = drawConceptRow(cs, y, "Ausencias injustificadas", payroll.getDescAusencias());
                y = drawConceptRow(cs, y, "Embargos", payroll.getDescEmbargos());

                drawSeparator(cs, y);
                y -= 8;
                cs.setFont(fontBold, 9);
                y = drawConceptRow(cs, y, "TOTAL DESCUENTOS", payroll.getTotalDescuentos());

                y -= 14;
                drawDoubleSeparator(cs, y);
                y -= 14;

                // ── Neto ────────────────────────────────────────────
                cs.setFont(fontBold, 12);
                y = drawConceptRow(cs, y, "NETO A PAGAR", payroll.getNetoApagar());

                y -= 14;
                drawSeparator(cs, y);
                y -= 24;

                // ── Footer ──────────────────────────────────────────
                cs.setFont(font, 7);
                String footer = "Este recibo es una constancia de pago. Conservar para cualquier reclamo.";
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(footer);
                cs.endText();

                // ── Estampilla de aprobación ────────────────────────
                if (payroll.getEstado() == Payroll.Estado.APROBADA && payroll.getFechaAprobacion() != null) {
                    y -= 20;
                    cs.setFont(fontBold, 9);
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText("APROBADO el " + payroll.getFechaAprobacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    cs.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar recibo de sueldo PDF", e);
        }
    }

    private int drawConceptRow(PDPageContentStream cs, int y, String concepto, BigDecimal importe) throws IOException {
        String importeStr = importe != null ? String.format("$ %,.2f", importe) : "$ 0.00";
        drawRow(cs, y, concepto, importeStr);
        return y - ROW_HEIGHT;
    }

    private void drawRow(PDPageContentStream cs, int y, String left, String right) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(left);
        cs.endText();

        float rightWidth = (fontBold != null ? fontBold : font).getStringWidth(right) / 1000 * 10;
        cs.beginText();
        cs.newLineAtOffset(PAGE_WIDTH - MARGIN - rightWidth, y);
        cs.showText(right);
        cs.endText();
    }

    private void drawSeparator(PDPageContentStream cs, int y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
    }

    private void drawDoubleSeparator(PDPageContentStream cs, int y) throws IOException {
        drawSeparator(cs, y);
        drawSeparator(cs, y - 3);
    }

    private PDFont loadFont(PDDocument doc) {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private PDFont loadFontBold(PDDocument doc) {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }
}
