package com.pos.system.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    @Test
    void encryptDecrypt_ShouldRoundTrip() {
        CryptoService service = new CryptoService("clave-de-prueba-para-tests");
        String plaintext = "MiPasswordDelKeystore#2026";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_ShouldProduceDifferentOutputPerCall() {
        CryptoService service = new CryptoService("clave-de-prueba-para-tests");

        String encrypted1 = service.encrypt("mismo-texto");
        String encrypted2 = service.encrypt("mismo-texto");

        // IV aleatorio por cifrado: mismo plaintext → distinto ciphertext
        assertNotEquals(encrypted1, encrypted2);
        assertEquals("mismo-texto", service.decrypt(encrypted1));
        assertEquals("mismo-texto", service.decrypt(encrypted2));
    }

    @Test
    void decrypt_WhenPayloadTampered_ShouldThrow() {
        CryptoService service = new CryptoService("clave-de-prueba-para-tests");
        String encrypted = service.encrypt("secreto-valioso");

        // Corromper el ciphertext: GCM detecta la manipulación
        char[] chars = encrypted.toCharArray();
        chars[chars.length - 2] = chars[chars.length - 2] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered));
    }

    @Test
    void decrypt_WithDifferentKey_ShouldThrow() {
        CryptoService encriptador = new CryptoService("clave-a");
        CryptoService desencriptador = new CryptoService("clave-b");

        String encrypted = encriptador.encrypt("secreto");

        assertThrows(IllegalStateException.class, () -> desencriptador.decrypt(encrypted));
    }

    @Test
    void constructor_WithBlankKey_ShouldUseDevFallback() {
        // Sin POS_CRYPTO_KEY definida (dev/tests) el servicio no explota:
        // usa una clave de fallback y funciona de forma determinista.
        CryptoService fallbackA = new CryptoService("");
        CryptoService fallbackB = new CryptoService("   ");

        String encrypted = fallbackA.encrypt("secreto-dev");

        assertDoesNotThrow(() -> fallbackA.decrypt(encrypted));
        // La misma clave de fallback es compatible entre instancias
        assertDoesNotThrow(() -> fallbackB.decrypt(encrypted));
    }
}