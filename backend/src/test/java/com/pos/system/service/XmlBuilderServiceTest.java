package com.pos.system.service;

import com.pos.system.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link XmlBuilderService}.
 * Verifica que el XML generado contenga los datos esperados.
 */
class XmlBuilderServiceTest {

    private final XmlBuilderService service = new XmlBuilderService();

    @Test
    void buildInvoiceXml_WithClient_ShouldContainAllSections() {
        Company company = Company.builder()
                .businessName("Mi Empresa SRL")
                .tradingName("Mi Empresa")
                .documentType("CUIT")
                .documentNumber("30-12345678-9")
                .taxAddress("Av. Corrientes 1234")
                .email("info@miempresa.com")
                .puntoVenta(1)
                .build();

        Client client = Client.builder()
                .name("Juan Pérez")
                .businessName("Juan Pérez SRL")
                .documentType("CUIT")
                .documentNumber("30-98765432-1")
                .condicionIva(CondicionIva.RESPONSABLE_INSCRIPTO)
                .taxAddress("Av. Siempre Viva 456")
                .build();

        Sale sale = Sale.builder()
                .subtotal(BigDecimal.valueOf(5000))
                .taxAmount(BigDecimal.valueOf(1050))
                .discount(BigDecimal.valueOf(500))
                .total(BigDecimal.valueOf(5550))
                .createdAt(LocalDateTime.of(2026, 5, 31, 14, 30))
                .items(List.of(
                        SaleItem.builder()
                                .productName("Producto A")
                                .quantity(2)
                                .unitPrice(new BigDecimal("1500.00"))
                                .discount(BigDecimal.ZERO)
                                .taxAmount(new BigDecimal("315.00"))
                                .subtotal(new BigDecimal("1815.00"))
                                .build(),
                        SaleItem.builder()
                                .productName("Producto B")
                                .quantity(1)
                                .unitPrice(new BigDecimal("2000.00"))
                                .discount(new BigDecimal("500.00"))
                                .taxAmount(BigDecimal.ZERO)
                                .subtotal(new BigDecimal("1500.00"))
                                .build()
                ))
                .build();

        String xml = service.buildInvoiceXml(sale, client, company, TipoComprobante.FACTURA_A, 1L);

        assertNotNull(xml);
        assertTrue(xml.contains("FACTURA_A"), "Debe contener el tipo de comprobante");
        assertTrue(xml.contains("Mi Empresa SRL"), "Debe contener la razón social del emisor");
        assertTrue(xml.contains("Juan Pérez SRL"), "Debe contener la razón social del receptor");
        assertTrue(xml.contains("RESPONSABLE_INSCRIPTO"), "Debe contener la condición IVA");
        assertTrue(xml.contains("Producto A"), "Debe contener el nombre del item");
        assertTrue(xml.contains("Producto B"), "Debe contener el nombre del segundo item");
        assertTrue(xml.contains("5550.00"), "Debe contener el importe total");
        assertTrue(xml.contains("5000.00"), "Debe contener el subtotal");
        assertTrue(xml.contains("1050.00"), "Debe contener el impuesto total");
        assertTrue(xml.contains("500.00"), "Debe contener el descuento total");
        assertTrue(xml.startsWith("<?xml"), "Debe comenzar con declaración XML");
    }

    @Test
    void buildInvoiceXml_WithoutClient_ShouldUseConsumidorFinal() {
        Company company = Company.builder()
                .businessName("Mi Empresa SRL")
                .documentType("CUIT")
                .documentNumber("30-12345678-9")
                .puntoVenta(1)
                .build();

        Sale sale = Sale.builder()
                .subtotal(BigDecimal.valueOf(1500))
                .taxAmount(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(1500))
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        SaleItem.builder()
                                .productName("Producto X")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(1500))
                                .discount(BigDecimal.ZERO)
                                .taxAmount(BigDecimal.ZERO)
                                .subtotal(BigDecimal.valueOf(1500))
                                .build()
                ))
                .build();

        // Client = null → Consumidor Final
        String xml = service.buildInvoiceXml(sale, null, company, TipoComprobante.BOLETA, 1L);

        assertNotNull(xml);
        assertTrue(xml.contains("BOLETA"));
        assertTrue(xml.contains("Consumidor Final"));
        assertTrue(xml.contains("CONSUMIDOR_FINAL"));
    }

    @Test
    void buildInvoiceXml_WithNullClientFields_ShouldNotThrow() {
        Company company = Company.builder()
                .businessName("Test Company")
                .puntoVenta(1)
                .build();

        Client client = Client.builder()
                .name("Cliente")
                .build(); // No businessName, no document, no condicionIva

        Sale sale = Sale.builder()
                .subtotal(BigDecimal.valueOf(100))
                .taxAmount(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        SaleItem.builder()
                                .productName("Item")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(100))
                                .discount(BigDecimal.ZERO)
                                .taxAmount(BigDecimal.ZERO)
                                .subtotal(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();

        assertDoesNotThrow(() -> {
            String xml = service.buildInvoiceXml(sale, client, company, TipoComprobante.BOLETA, 1L);
            assertTrue(xml.contains("Cliente"));
        });
    }
}
