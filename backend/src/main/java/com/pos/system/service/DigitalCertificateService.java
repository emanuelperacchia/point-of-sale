package com.pos.system.service;

import com.pos.system.dto.response.DigitalCertificateResponse;
import com.pos.system.entity.DigitalCertificate;
import com.pos.system.repository.DigitalCertificateRepository;
import com.pos.system.security.CryptoService;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.List;

/**
 * Gestión de certificados digitales (PKCS#12) para facturación electrónica.
 * <p>
 * La contraseña del keystore se cifra con {@link CryptoService} ANTES de
 * persistirse; nunca se almacena ni se retorna en claro. Solo un certificado
 * puede estar activo a la vez (consistente con
 * {@code DigitalCertificateRepository.findByActiveTrue()}).
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DigitalCertificateService {

    private static final String KEYSTORE_TYPE = "PKCS12";

    private final DigitalCertificateRepository certificateRepository;
    private final CryptoService cryptoService;

    /**
     * Carga un certificado PKCS#12 y lo deja como activo (desactiva el anterior).
     *
     * @param file          archivo .p12 / .pfx
     * @param storePassword contraseña del keystore (se cifra antes de persistir)
     * @param alias         nombre identificador del certificado
     */
    @Transactional
    public DigitalCertificateResponse upload(MultipartFile file, String storePassword, String alias) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar el archivo .p12 del certificado");
        }
        if (storePassword == null || storePassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña del keystore es obligatoria");
        }
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("El alias es obligatorio");
        }

        // Validar el keystore ANTES de persistir: password incorrecta falla acá,
        // no cuando se intente firmar el primer comprobante.
        X509Certificate cert = loadAndValidate(file, storePassword);

        // Desactivar el certificado activo anterior (solo uno activo a la vez)
        certificateRepository.findByActiveTrue().ifPresent(previous -> {
            previous.setActive(false);
            certificateRepository.save(previous);
        });

        byte[] certificateData;
        try {
            certificateData = file.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo del certificado", e);
        }

        DigitalCertificate entity = DigitalCertificate.builder()
                .alias(alias)
                .storeType(KEYSTORE_TYPE)
                .storePassword(cryptoService.encrypt(storePassword))
                .certificateData(certificateData)
                .validFrom(cert.getNotBefore().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime())
                .validTo(cert.getNotAfter().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime())
                .issuer(cert.getIssuerX500Principal().getName())
                .subject(cert.getSubjectX500Principal().getName())
                .serialNumber(cert.getSerialNumber().toString(16))
                .active(true)
                .build();

        return mapToResponse(certificateRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public DigitalCertificateResponse getActive() {
        return certificateRepository.findByActiveTrue()
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DigitalCertificateResponse> listAll() {
        return certificateRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private X509Certificate loadAndValidate(MultipartFile file, String storePassword) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, "BC");
            ks.load(new ByteArrayInputStream(file.getBytes()), storePassword.toCharArray());
            String aliasName = ks.aliases().nextElement();
            return (X509Certificate) ks.getCertificate(aliasName);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Archivo inválido o contraseña incorrecta: " + e.getMessage(), e);
        }
    }

    private DigitalCertificateResponse mapToResponse(DigitalCertificate cert) {
        return DigitalCertificateResponse.builder()
                .id(cert.getId())
                .alias(cert.getAlias())
                .storeType(cert.getStoreType())
                .validFrom(cert.getValidFrom())
                .validTo(cert.getValidTo())
                .issuer(cert.getIssuer())
                .subject(cert.getSubject())
                .serialNumber(cert.getSerialNumber())
                .active(cert.getActive())
                .createdAt(cert.getCreatedAt())
                .build();
    }
}