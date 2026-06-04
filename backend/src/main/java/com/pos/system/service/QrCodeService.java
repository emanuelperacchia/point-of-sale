package com.pos.system.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pos.system.entity.InvoiceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Genera códigos QR para comprobantes electrónicos.
 * <p>
 * El QR contiene los datos mínimos de verificación según normativa AFIP:
 * tipo de comprobante, punto de venta, número, CAE, fecha de emisión,
 * documento del receptor e importe total.
 * </p>
 */
@Service
public class QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);
    private static final int QR_SIZE = 200;

    /**
     * Genera un código QR en formato Base64 (PNG) para el comprobante.
     *
     * @param invoice comprobante electrónico emitido
     * @return imagen PNG codificada en Base64
     */
    public String generateQrBase64(InvoiceDocument invoice) {
        try {
            String data = buildQrContent(invoice);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            // Convertir a BufferedImage y luego a Base64
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);

            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            log.debug("QR generado para comprobante {} ({} bytes base64)", invoice.getNumero(), base64.length());
            return base64;

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Error al generar código QR para comprobante " + invoice.getNumero(), e);
        }
    }

    /**
     * Construye el contenido del QR.
     * <p>
     * En producción con AFIP, debe seguir el formato:
     * {@code https://www.afip.gob.ar/fe/qr/?p=...}
     * Para el mock, usamos un texto legible con los datos del comprobante.
     * </p>
     */
    private String buildQrContent(InvoiceDocument invoice) {
        // Formato simplificado para mock — en producción usar URL de AFIP
        return String.format(
                "Comprobante: %s | PV: %d | Nro: %d | CAE: %s | Vto: %s | Receptor: %s | Doc: %s",
                invoice.getTipoComprobante(),
                invoice.getPuntoVenta(),
                invoice.getNumero(),
                invoice.getCae() != null ? invoice.getCae() : "—",
                invoice.getFechaCae() != null ? invoice.getFechaCae().toLocalDate() : "—",
                invoice.getReceptorNombre() != null ? invoice.getReceptorNombre() : "—",
                invoice.getReceptorDocumento() != null ? invoice.getReceptorDocumento() : "—"
        );
    }
}
