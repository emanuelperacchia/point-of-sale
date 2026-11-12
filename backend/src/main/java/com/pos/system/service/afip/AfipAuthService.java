package com.pos.system.service.afip;

import com.pos.system.config.FiscalProperties;
import com.pos.system.entity.DigitalCertificate;
import com.pos.system.repository.DigitalCertificateRepository;
import com.pos.system.security.CryptoService;
import com.pos.system.service.soap.SoapClient;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autenticación contra AFIP mediante WSAA (Web Services de Autenticación y
 * Autorización).
 * <p>
 * Flujo:
 * <ol>
 *   <li>Genera un {@code LoginTicketRequest} (XML con {@code uniqueId},
 *       tiempos de generación/expiración y servicio {@code wsfe}).</li>
 *   <li>Lo firma con CMS/PKCS#7 usando la clave privada del certificado
 *       digital (.p12) activo, vía Bouncy Castle.</li>
 *   <li>Lo envía al servicio {@code LoginCms} de AFIP y obtiene el
 *       {@code ticketAccess} (Ticket de Acceso codificado en base64).</li>
 *   <li>Lo caches hasta ~5 minutos antes de expirar (AFIP da ~12h de vigencia).</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AfipAuthService {

    private static final Logger log = LoggerFactory.getLogger(AfipAuthService.class);

    private static final String SERVICE = "wsfe";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static final Pattern TICKET_ACCESS_PATTERN =
            Pattern.compile("<ticketAccess>(.*?)</ticketAccess>", Pattern.DOTALL);
    private static final Pattern EXPIRATION_PATTERN =
            Pattern.compile("<expirationTime>(.*?)</expirationTime>", Pattern.DOTALL);

    private final DigitalCertificateRepository certificateRepository;
    private final CryptoService cryptoService;
    private final SoapClient soapClient;
    private final FiscalProperties fiscalProperties;

    private volatile String cachedTicketAccess;
    private volatile LocalDateTime ticketExpiry;

    @PostConstruct
    void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Devuelve un Ticket de Acceso válido, renovándolo si está próximo a expirar.
     */
    public String getTicketAccess() {
        if (cachedTicketAccess != null && ticketExpiry != null
                && LocalDateTime.now().isBefore(ticketExpiry.minusMinutes(5))) {
            return cachedTicketAccess;
        }
        return renewTicketAccess();
    }

    /**
     * Renueva el Ticket de Acceso consultando WSAA de AFIP.
     */
    @Scheduled(fixedDelayString = "${afip.ta.refresh-interval-ms:3600000}")
    public synchronized String renewTicketAccess() {
        try {
            DigitalCertificate cert = certificateRepository.findByActiveTrue()
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay certificado digital activo para autenticarse en AFIP " +
                                    "(cargalo en POST /api/fiscal/certificates)"));

            Instant now = Instant.now();
            LocalDateTime generationTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault())
                    .minusMinutes(10); // tolerancia de reloj del servidor AFIP
            LocalDateTime expirationTime = generationTime.plusHours(12);

            String uniqueId = String.valueOf(now.toEpochMilli());
            String loginTicketXml = buildLoginTicketRequest(uniqueId, generationTime, expirationTime);

            String signedBase64 = signCms(loginTicketXml, cert);
            String soapEnvelope = buildWsaaSoapEnvelope(signedBase64);
            String endpoint = wsaaEndpoint();

            String response = soapClient.post(endpoint, "loginTicketRequest", soapEnvelope);
            parseTicketAccess(response);

            log.info("Ticket de acceso AFIP obtenido (expira {})", ticketExpiry);
            return cachedTicketAccess;
        } catch (Exception e) {
            log.error("Error obteniendo Ticket de Acceso de AFIP", e);
            throw new IllegalStateException("No se pudo autenticar con AFIP (WSAA): " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // 1. LoginTicketRequest XML
    // ──────────────────────────────────────────────

    private String buildLoginTicketRequest(String uniqueId,
                                           LocalDateTime generationTime,
                                           LocalDateTime expirationTime) {
        return """
                <loginTicketRequest>
                  <header>
                    <uniqueId>%s</uniqueId>
                    <generationTime>%s</generationTime>
                    <expirationTime>%s</expirationTime>
                  </header>
                  <service>%s</service>
                </loginTicketRequest>
                """.formatted(
                uniqueId,
                generationTime.format(TS_FORMATTER),
                expirationTime.format(TS_FORMATTER),
                SERVICE
        );
    }

    // ──────────────────────────────────────────────
    // 2. Firma CMS/PKCS#7 con Bouncy Castle
    // ──────────────────────────────────────────────

    private String signCms(String content, DigitalCertificate certificate) throws Exception {
        char[] password = cryptoService.decrypt(certificate.getStorePassword()).toCharArray();
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, "BC");
        ks.load(new ByteArrayInputStream(certificate.getCertificateData()), password);

        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password);
        java.security.cert.X509Certificate cert =
                (java.security.cert.X509Certificate) ks.getCertificate(alias);

        X509CertificateHolder certHolder = new X509CertificateHolder(cert.getEncoded());

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);

        generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                )
                .build(signer, certHolder)
        );
        generator.addCertificate(certHolder);

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        org.bouncycastle.cms.CMSTypedData typedData =
                new org.bouncycastle.cms.CMSProcessableByteArray(contentBytes);
        CMSSignedData signedData = generator.generate(typedData, true);

        return Base64.getEncoder().encodeToString(signedData.getEncoded());
    }

    // ──────────────────────────────────────────────
    // 3. Sobre SOAP para WSAA
    // ──────────────────────────────────────────────

    private String buildWsaaSoapEnvelope(String signedBase64) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:ws="http://wsaa.view.service.dgi.afip.gov.ar/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <ws:loginTicketRequest>
                      <ws:in0>%s</ws:in0>
                    </ws:loginTicketRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(signedBase64);
    }

    private String wsaaEndpoint() {
        return switch (fiscalProperties.getMode()) {
            case "produccion" -> "https://wsaa.afip.gov.ar/ws/services/LoginCms";
            case "homologacion" -> "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
            default -> throw new IllegalStateException(
                    "modo fiscal '" + fiscalProperties.getMode()
                            + "' no válido para WSAA (use homologacion o produccion)");
        };
    }

    // ──────────────────────────────────────────────
    // 4. Parseo del Ticket de Acceso
    // ──────────────────────────────────────────────

    private void parseTicketAccess(String soapResponse) {
        Matcher matcher = TICKET_ACCESS_PATTERN.matcher(soapResponse);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "AFIP WSAA no devolvió ticketAccess. Respuesta: " + soapResponse);
        }

        String ticketAccessBase64 = matcher.group(1).trim();
        String ticketXml;
        try {
            ticketXml = new String(Base64.getDecoder().decode(ticketAccessBase64),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "ticketAccess de AFIP no es base64 válido: " + e.getMessage(), e);
        }

        Matcher expMatcher = EXPIRATION_PATTERN.matcher(ticketXml);
        if (!expMatcher.find()) {
            throw new IllegalStateException(
                    "ticketAccess de AFIP no contiene expirationTime: " + ticketXml);
        }

        this.cachedTicketAccess = ticketAccessBase64;
        this.ticketExpiry = LocalDateTime.parse(expMatcher.group(1).trim(), TS_FORMATTER);
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
