package com.pos.system.service;

import com.pos.system.entity.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;

/**
 * Construye el XML del comprobante electrónico en formato compatible
 * con los organismos fiscales (AFIP/SII).
 * <p>
 * El XML generado incluye cabecera, datos del emisor y receptor,
 * detalle de items y totales.
 * </p>
 */
@Service
public class XmlBuilderService {

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Genera el XML completo para un comprobante electrónico.
     *
     * @param sale   venta asociada
     * @param client cliente (puede ser null para consumidor final)
     * @param company datos de la empresa emisora
     * @param tipo   tipo de comprobante
     * @param numero número correlativo asignado
     * @return XML como String
     */
    public String buildInvoiceXml(Sale sale, Client client, Company company,
                                  TipoComprobante tipo, Long numero) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();

            Element root = doc.createElement("ComprobanteElectronico");
            doc.appendChild(root);

            // ── Cabecera ──
            Element cabecera = doc.createElement("Cabecera");
            addChild(doc, cabecera, "TipoComprobante", tipo.name());
            addChild(doc, cabecera, "PuntoVenta", String.valueOf(company.getPuntoVenta()));
            addChild(doc, cabecera, "Numero", String.valueOf(numero));
            addChild(doc, cabecera, "FechaEmision", sale.getCreatedAt().toLocalDate().toString());
            addChild(doc, cabecera, "HoraEmision", sale.getCreatedAt().toLocalTime().format(HOUR_FORMATTER));
            addChild(doc, cabecera, "Moneda", "ARS");
            root.appendChild(cabecera);

            // ── Emisor ──
            Element emisor = doc.createElement("Emisor");
            addChild(doc, emisor, "RazonSocial", company.getBusinessName());
            addChild(doc, emisor, "NombreFantasia", company.getTradingName());
            addChild(doc, emisor, "DocumentoTipo",
                    company.getDocumentType() != null ? company.getDocumentType() : "CUIT");
            addChild(doc, emisor, "DocumentoNumero",
                    company.getDocumentNumber() != null ? company.getDocumentNumber() : "");
            addChild(doc, emisor, "DomicilioFiscal",
                    company.getTaxAddress() != null ? company.getTaxAddress() : "");
            addChild(doc, emisor, "Email", company.getEmail());
            addChild(doc, emisor, "Telefono", company.getPhone());
            root.appendChild(emisor);

            // ── Receptor ──
            Element receptor = doc.createElement("Receptor");
            String receptorName;
            if (client != null) {
                receptorName = client.getBusinessName() != null
                        ? client.getBusinessName()
                        : client.getName();
            } else {
                receptorName = "Consumidor Final";
            }
            addChild(doc, receptor, "RazonSocial", receptorName);
            addChild(doc, receptor, "DocumentoTipo",
                    client != null && client.getDocumentType() != null
                            ? client.getDocumentType() : "DNI");
            addChild(doc, receptor, "DocumentoNumero",
                    client != null && client.getDocumentNumber() != null
                            ? client.getDocumentNumber() : "0");
            addChild(doc, receptor, "CondicionIVA",
                    client != null ? client.getCondicionIva().name() : "CONSUMIDOR_FINAL");
            addChild(doc, receptor, "DomicilioFiscal",
                    client != null && client.getTaxAddress() != null
                            ? client.getTaxAddress() : "");
            root.appendChild(receptor);

            // ── Detalle (items) ──
            Element detalle = doc.createElement("Detalle");
            for (SaleItem item : sale.getItems()) {
                Element itemEl = doc.createElement("Item");
                addChild(doc, itemEl, "Descripcion", item.getProductName());
                addChild(doc, itemEl, "Cantidad", String.valueOf(item.getQuantity()));
                addChild(doc, itemEl, "UnidadMedida", "unidad");
                addChild(doc, itemEl, "PrecioUnitario",
                        item.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                addChild(doc, itemEl, "Descuento",
                        item.getDiscount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                addChild(doc, itemEl, "Impuesto",
                        item.getTaxAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                addChild(doc, itemEl, "Subtotal",
                        item.getSubtotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                detalle.appendChild(itemEl);
            }
            root.appendChild(detalle);

            // ── Totales ──
            Element totales = doc.createElement("Totales");
            addChild(doc, totales, "Subtotal",
                    sale.getSubtotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            addChild(doc, totales, "ImpuestoTotal",
                    sale.getTaxAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            addChild(doc, totales, "DescuentoTotal",
                    sale.getDiscount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            addChild(doc, totales, "ImporteTotal",
                    sale.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            root.appendChild(totales);

            // ── Convertir a string ──
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar XML del comprobante", e);
        }
    }

    private void addChild(Document doc, Element parent, String name, String value) {
        Element el = doc.createElement(name);
        el.setTextContent(value != null ? value : "");
        parent.appendChild(el);
    }
}
