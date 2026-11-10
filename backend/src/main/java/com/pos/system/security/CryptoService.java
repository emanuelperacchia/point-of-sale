package com.pos.system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cifrado simétrico AES-256-GCM para secretos persistentes.
 * <p>
 * Aplica a valores que DEBEN almacenarse en base de datos pero no deben
 * leerse en claro (p. ej. {@code DigitalCertificate.storePassword}).
 * La clave se deriva determinísticamente (SHA-256) del valor de entorno
 * {@code POS_CRYPTO_KEY}: esto permite usar cualquier longitud de clave
 * manteniendo AES-256.
 * </p>
 * <p>
 * GCM autentica el ciphertext: cualquier manipulación del valor persistido
 * lanza excepción al descifrar (fail fast, sin datos corruptos silenciosos).
 * </p>
 */
@Component
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;          // 96 bits, recomendado por NIST
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] keyBytes;

    public CryptoService(@Value("${pos.crypto.key:}") String cryptoKey) {
        if (cryptoKey == null || cryptoKey.isBlank()) {
            // Fallback seguro para desarrollo y tests si no se definió variable de entorno
            cryptoKey = "dev-fallback-crypto-key-insegura-solo-para-pruebas";
        }
        this.keyBytes = sha256(cryptoKey.getBytes(StandardCharsets.UTF_8));
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV + ciphertext(tag incluido) → base64
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar secreto", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted);
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Payload cifrado demasiado corto");
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            byte[] ciphertext = new byte[payload.length - iv.length];
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al descifrar secreto: clave inválida o payload manipulado", e);
        }
    }

    private SecretKeySpec keySpec() {
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    // Exposición para debugging/validación de formato (sin revelar material clave)
    String derivedKeyFingerprint() {
        return HexFormat.of().formatHex(keyBytes, 0, 4);
    }
}