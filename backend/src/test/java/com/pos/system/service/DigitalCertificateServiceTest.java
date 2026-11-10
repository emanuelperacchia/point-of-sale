package com.pos.system.service;

import com.pos.system.dto.response.DigitalCertificateResponse;
import com.pos.system.entity.DigitalCertificate;
import com.pos.system.repository.DigitalCertificateRepository;
import com.pos.system.security.CryptoService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalCertificateServiceTest {

    private static final String STORE_PASSWORD = "MiPasswordSecreto#2026";
    private static final String CRYPTO_KEY = "clave-de-prueba-para-tests";

    @Mock
    private DigitalCertificateRepository certificateRepository;

    private DigitalCertificateService service;
    private CryptoService cryptoService;

    @BeforeEach
    void setUp() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        cryptoService = new CryptoService(CRYPTO_KEY);
        service = new DigitalCertificateService(certificateRepository, cryptoService);
    }

    @Test
    void upload_ShouldEncryptStorePasswordAndExtractMetadata() throws Exception {
        byte[] p12 = createTestP12(STORE_PASSWORD);
        MockMultipartFile file = new MockMultipartFile("file", "cert.p12",
                "application/x-pkcs12", p12);

        when(certificateRepository.findByActiveTrue()).thenReturn(Optional.empty());
        DigitalCertificate saved = DigitalCertificate.builder()
                .id(1L).alias("afip-prod").active(true).build();
        when(certificateRepository.save(any(DigitalCertificate.class))).thenReturn(saved);

        DigitalCertificateResponse response = service.upload(file, STORE_PASSWORD, "afip-prod");

        assertNotNull(response);

        ArgumentCaptor<DigitalCertificate> captor = ArgumentCaptor.forClass(DigitalCertificate.class);
        verify(certificateRepository).save(captor.capture());
        DigitalCertificate persisted = captor.getValue();

        // El password NUNCA se persiste en claro
        assertNotEquals(STORE_PASSWORD, persisted.getStorePassword());
        // Y descifra de vuelta al mismo valor (round-trip AES-GCM)
        assertEquals(STORE_PASSWORD, cryptoService.decrypt(persisted.getStorePassword()));

        // Los metadatos se extraen del X.509 real
        assertNotNull(persisted.getIssuer());
        assertNotNull(persisted.getSubject());
        assertNotNull(persisted.getSerialNumber());
        assertNotNull(persisted.getValidFrom());
        assertNotNull(persisted.getValidTo());
        assertEquals("PKCS12", persisted.getStoreType());
        assertEquals(true, persisted.getActive());
    }

    @Test
    void upload_WithWrongPassword_ShouldReject() throws Exception {
        byte[] p12 = createTestP12(STORE_PASSWORD);
        MockMultipartFile file = new MockMultipartFile("file", "cert.p12",
                "application/x-pkcs12", p12);

        assertThrows(IllegalArgumentException.class,
                () -> service.upload(file, "password-incorrecta", "afip-prod"));
        // Nada se persistió
        verify(certificateRepository, never()).save(any());
    }

    @Test
    void upload_SecondCertificate_ShouldDeactivatePrevious() throws Exception {
        byte[] p12 = createTestP12(STORE_PASSWORD);
        MockMultipartFile file = new MockMultipartFile("file", "cert.p12",
                "application/x-pkcs12", p12);

        DigitalCertificate previous = DigitalCertificate.builder()
                .id(1L).alias("cert-viejo").active(true).build();
        when(certificateRepository.findByActiveTrue()).thenReturn(Optional.of(previous));
        when(certificateRepository.save(any(DigitalCertificate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upload(file, STORE_PASSWORD, "cert-nuevo");

        assertFalse(previous.getActive());
        verify(certificateRepository).save(previous);
        verify(certificateRepository).save(argThat(c -> "cert-nuevo".equals(c.getAlias())));
    }

    @Test
    void upload_WithEmptyFile_ShouldReject() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.p12",
                "application/x-pkcs12", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.upload(empty, STORE_PASSWORD, "alias"));
    }

    // --- helpers: genera un PKCS#12 autofirmado en memoria ---

    private byte[] createTestP12(String password) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=Nivex Test, O=Nivex S.A., C=AR");
        Instant now = Instant.now();
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(System.currentTimeMillis()),
                Date.from(now.minusSeconds(3600)),
                Date.from(now.plusSeconds(31536000)), // 1 año
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                certBuilder.build(signer));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("nivex-test", keyPair.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[]{cert});

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ks.store(out, password.toCharArray());
        return out.toByteArray();
    }
}