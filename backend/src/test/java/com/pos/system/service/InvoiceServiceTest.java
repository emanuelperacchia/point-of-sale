package com.pos.system.service;

import com.pos.system.config.FiscalProperties;
import com.pos.system.dto.response.InvoiceResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import com.pos.system.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private DigitalCertificateRepository certificateRepository;
    @Mock private XmlBuilderService xmlBuilder;
    @Mock private DigitalSignatureService digitalSignatureService;
    @Mock private FiscalApiClient fiscalApiClient;
    @Mock private FiscalProperties fiscalProperties;
    @Mock private PdfGeneratorService pdfGenerator;
    @Mock private QrCodeService qrCodeService;

    private InvoiceServiceImpl invoiceService;

    private Sale sale;
    private Client client;
    private Company company;
    private FiscalProperties.Retry retryConfig;
    private FiscalProperties.Pdf pdfConfig;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(
                saleRepository, invoiceRepository, companyRepository,
                certificateRepository, xmlBuilder, digitalSignatureService,
                fiscalApiClient, fiscalProperties, pdfGenerator, qrCodeService
        );

        client = Client.builder()
                .id(1L).name("Juan Pérez")
                .documentType("DNI").documentNumber("12.345.678")
                .condicionIva(CondicionIva.CONSUMIDOR_FINAL)
                .build();

        company = Company.builder()
                .id(1L).businessName("Mi Empresa SRL")
                .documentType("CUIT").documentNumber("30-12345678-9")
                .puntoVenta(1).taxAddress("Av. Siempre Viva 123")
                .active(true)
                .build();

        sale = Sale.builder()
                .id(1L).client(client)
                .subtotal(BigDecimal.valueOf(3000))
                .taxAmount(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(3000))
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        SaleItem.builder()
                                .id(1L).productName("Sándwich de Miga")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(1500))
                                .discount(BigDecimal.ZERO)
                                .subtotal(BigDecimal.valueOf(3000))
                                .build()
                ))
                .build();

        retryConfig = new FiscalProperties.Retry();
        retryConfig.setMaxAttempts(3);
        pdfConfig = new FiscalProperties.Pdf();
        pdfConfig.setOutputDir("./target/test-invoices");
    }

    // ──────────────────────────────────────────────
    // emitInvoice
    // ──────────────────────────────────────────────

    @Test
    void emitInvoice_WhenAlreadyExists_ShouldReturnExisting() {
        InvoiceDocument existing = InvoiceDocument.builder()
                .id(1L).saleId(1L)
                .tipoComprobante(TipoComprobante.BOLETA)
                .numero(1L).estado(InvoiceStatus.EMITIDO)
                .build();

        when(invoiceRepository.findBySaleId(1L)).thenReturn(Optional.of(existing));

        InvoiceResponse result = invoiceService.emitInvoice(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(InvoiceStatus.EMITIDO, result.estado());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void emitInvoice_WhenSaleNotFound_ShouldThrowException() {
        when(invoiceRepository.findBySaleId(99L)).thenReturn(Optional.empty());
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.emitInvoice(99L));
    }

    @Test
    void emitInvoice_WhenNoCompany_ShouldThrowException() {
        when(invoiceRepository.findBySaleId(1L)).thenReturn(Optional.empty());
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(companyRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.emitInvoice(1L));
    }

    @Test
    void emitInvoice_WithMockSuccess_ShouldEmitBoletaForConsumidorFinal() {
        // Given
        when(invoiceRepository.findBySaleId(1L)).thenReturn(Optional.empty());
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(companyRepository.findByActiveTrue()).thenReturn(Optional.of(company));
        when(invoiceRepository.nextSequenceValue("BOLETA")).thenReturn(1L);
        when(xmlBuilder.buildInvoiceXml(any(), any(), any(), any(), anyLong())).thenReturn("<xml/>");
        when(certificateRepository.findByActiveTrue()).thenReturn(Optional.empty());

        FiscalApiClient.FiscalEmissionResponse fiscalResponse =
                new FiscalApiClient.FiscalEmissionResponse(
                        "12345678901", LocalDateTime.now().plusDays(15),
                        "A", "Aprobado", "<response/>");
        when(fiscalApiClient.emitirComprobante(anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(fiscalResponse);

        InvoiceDocument saved = InvoiceDocument.builder()
                .id(1L).saleId(1L).tipoComprobante(TipoComprobante.BOLETA)
                .puntoVenta(1).numero(1L)
                .cae("12345678901").estado(InvoiceStatus.EMITIDO)
                .build();
        when(invoiceRepository.save(any(InvoiceDocument.class))).thenReturn(saved);

        // When
        InvoiceResponse result = invoiceService.emitInvoice(1L);

        // Then
        assertNotNull(result);
        assertEquals(TipoComprobante.BOLETA, result.tipoComprobante());
        assertEquals(InvoiceStatus.EMITIDO, result.estado());
        assertEquals("12345678901", result.cae());

        verify(xmlBuilder).buildInvoiceXml(eq(sale), eq(client), eq(company), eq(TipoComprobante.BOLETA), eq(1L));
        verify(fiscalApiClient).emitirComprobante(anyString(), eq("BOLETA"), eq(1L), eq(1));
        verify(invoiceRepository).save(any(InvoiceDocument.class));
    }

    // ──────────────────────────────────────────────
    // retryEmission
    // ──────────────────────────────────────────────

    @Test
    void retryEmission_WhenNotPending_ShouldThrowException() {
        InvoiceDocument invoice = InvoiceDocument.builder()
                .id(1L).estado(InvoiceStatus.EMITIDO).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThrows(BadRequestException.class, () -> invoiceService.retryEmission(1L));
    }

    @Test
    void retryEmission_WhenMaxAttemptsReached_ShouldMarkRejected() {
        InvoiceDocument invoice = InvoiceDocument.builder()
                .id(1L).estado(InvoiceStatus.PENDIENTE).intentos(3).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(fiscalProperties.getRetry()).thenReturn(retryConfig);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        InvoiceResponse result = invoiceService.retryEmission(1L);

        assertEquals(InvoiceStatus.RECHAZADO, result.estado());
        assertTrue(result.motivoRechazo().contains("Máximo de reintentos"));
        verify(fiscalApiClient, never()).emitirComprobante(anyString(), anyString(), anyLong(), anyInt());
    }

    @Test
    void retryEmission_WithSuccess_ShouldUpdateStatus() {
        InvoiceDocument invoice = InvoiceDocument.builder()
                .id(1L).saleId(1L)
                .tipoComprobante(TipoComprobante.FACTURA_B)
                .puntoVenta(1).numero(1L)
                .xmlFirmado("<firmado/>")
                .estado(InvoiceStatus.PENDIENTE)
                .intentos(1)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(fiscalProperties.getRetry()).thenReturn(retryConfig);
        when(companyRepository.findByActiveTrue()).thenReturn(Optional.of(company));

        FiscalApiClient.FiscalEmissionResponse fiscalResponse =
                new FiscalApiClient.FiscalEmissionResponse(
                        "98765432101", LocalDateTime.now().plusDays(15),
                        "A", "Aprobado", "<response/>");
        when(fiscalApiClient.emitirComprobante(anyString(), anyString(), anyLong(), anyInt()))
                .thenReturn(fiscalResponse);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        InvoiceResponse result = invoiceService.retryEmission(1L);

        assertEquals(InvoiceStatus.EMITIDO, result.estado());
        assertEquals("98765432101", result.cae());
        verify(invoiceRepository).save(any());
    }

    // ──────────────────────────────────────────────
    // getById
    // ──────────────────────────────────────────────

    @Test
    void getById_WhenNotFound_ShouldThrowException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getById(99L));
    }

    @Test
    void getById_WhenExists_ShouldReturnInvoice() {
        InvoiceDocument doc = InvoiceDocument.builder()
                .id(1L).saleId(1L)
                .tipoComprobante(TipoComprobante.BOLETA)
                .numero(1L).estado(InvoiceStatus.EMITIDO)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(doc));

        InvoiceResponse result = invoiceService.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    // ──────────────────────────────────────────────
    // findByFilters
    // ──────────────────────────────────────────────

    @Test
    void findByFilters_ShouldReturnList() {
        when(invoiceRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        InvoiceDocument.builder().id(1L).build()
                ));

        var result = invoiceService.findByFilters(null, null, null, null, null);
        assertEquals(1, result.size());
    }

    // ──────────────────────────────────────────────
    // getPdf
    // ──────────────────────────────────────────────

    @Test
    void getPdf_WhenNoPdfPath_ShouldThrowException() {
        InvoiceDocument doc = InvoiceDocument.builder().id(1L).pdfPath(null).build();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThrows(BadRequestException.class, () -> invoiceService.getPdf(1L));
    }

    @Test
    void getPdf_WhenFileNotFound_ShouldThrowException() {
        InvoiceDocument doc = InvoiceDocument.builder()
                .id(1L).pdfPath("/nonexistent/invoice.pdf").build();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getPdf(1L));
    }

    @Test
    void getPdf_WhenFileExists_ShouldReturnResource() throws Exception {
        // Create a temp file to simulate the PDF
        Path tempFile = Files.createTempFile("invoice-test-", ".pdf");
        Files.writeString(tempFile, "fake-pdf-content");

        try {
            InvoiceDocument doc = InvoiceDocument.builder()
                    .id(1L).pdfPath(tempFile.toAbsolutePath().toString()).build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(doc));

            Resource resource = invoiceService.getPdf(1L);
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertTrue(resource.isReadable());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ──────────────────────────────────────────────
    // generateInvoiceDeliverables
    // ──────────────────────────────────────────────

    @Test
    void generateInvoiceDeliverables_ShouldGeneratePdfAndQr() throws Exception {
        Files.createDirectories(Path.of("./target/test-invoices"));

        InvoiceDocument invoice = InvoiceDocument.builder()
                .id(1L).saleId(1L)
                .tipoComprobante(TipoComprobante.BOLETA)
                .numero(1L).estado(InvoiceStatus.EMITIDO)
                .cae("12345678901")
                .receptorNombre("Juan Pérez")
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(companyRepository.findByActiveTrue()).thenReturn(Optional.of(company));
        when(pdfGenerator.generateInvoicePdf(any(), any(), any()))
                .thenReturn("./target/test-invoices/test.pdf");
        when(qrCodeService.generateQrBase64(any()))
                .thenReturn("base64-qr-code-data");

        invoiceService.generateInvoiceDeliverables(1L);

        verify(pdfGenerator).generateInvoicePdf(invoice, sale, company);
        verify(qrCodeService).generateQrBase64(invoice);
        verify(invoiceRepository).save(argThat(doc ->
                doc.getPdfPath() != null && doc.getQrCode() != null));
    }
}
