package team.carrypigeon.backend.connection.protocol.encryption.aes;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AESUtilTests {

    @Test
    void generateKey_shouldReturn128BitKey() {
        new AESUtil();
        SecretKey key = AESUtil.generateKey();
        assertNotNull(key);
        assertNotNull(key.getEncoded());
        assertEquals(16, key.getEncoded().length);
    }

    @Test
    void encryptDecrypt_withAad_shouldRoundTrip() throws Exception {
        byte[] keyBytes = AESUtil.generateKey().getEncoded();
        byte[] aad = "aad".getBytes(StandardCharsets.UTF_8);

        AESData encrypted = AESUtil.encryptWithAAD("payload", aad, keyBytes);
        assertNotNull(encrypted);
        assertNotNull(encrypted.ciphertext());
        assertNotNull(encrypted.nonce());
        assertEquals(12, encrypted.nonce().length);

        String decrypted = AESUtil.decryptWithAAD(encrypted.ciphertext(), encrypted.nonce(), aad, keyBytes);
        assertEquals("payload", decrypted);
    }

    @Test
    void encryptDecrypt_withoutAad_shouldRoundTrip() throws Exception {
        byte[] keyBytes = AESUtil.generateKey().getEncoded();

        AESData encrypted = AESUtil.encryptWithAAD("payload", null, keyBytes);
        String decrypted = AESUtil.decryptWithAAD(encrypted.ciphertext(), encrypted.nonce(), null, keyBytes);
        assertEquals("payload", decrypted);
    }

    @Test
    void decrypt_wrongKey_shouldThrowException() throws Exception {
        byte[] keyBytes = AESUtil.generateKey().getEncoded();
        byte[] wrongKeyBytes = AESUtil.generateKey().getEncoded();
        byte[] aad = "aad".getBytes(StandardCharsets.UTF_8);

        AESData encrypted = AESUtil.encryptWithAAD("payload", aad, keyBytes);
        assertThrows(Exception.class, () -> AESUtil.decryptWithAAD(encrypted.ciphertext(), encrypted.nonce(), aad, wrongKeyBytes));
    }

    @Test
    void generateKey_withInvalidAlgorithm_shouldThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> AESUtil.generateKey("NO_SUCH_ALGORITHM"));
    }
}
