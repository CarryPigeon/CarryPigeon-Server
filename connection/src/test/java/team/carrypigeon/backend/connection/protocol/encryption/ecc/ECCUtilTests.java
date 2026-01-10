package team.carrypigeon.backend.connection.protocol.encryption.ecc;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ECCUtilTests {

    @Test
    void encryptAndDecrypt_shouldRoundTrip() throws Exception {
        new ECCUtil();
        KeyPair pair = ECCUtil.generateEccKeyPair();
        String plain = "hello ecc";

        byte[] encrypted = ECCUtil.encrypt(plain, pair.getPublic());
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);

        String decrypted = ECCUtil.decrypt(encrypted, pair.getPrivate());
        assertEquals(plain, decrypted);
    }

    @Test
    void rebuildPublicAndPrivateKey_shouldMatchOriginalEncodedBytes() throws Exception {
        KeyPair pair = ECCUtil.generateEccKeyPair();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        PublicKey rebuiltPublic = ECCUtil.rebuildPublicKey(publicKeyBase64);
        PrivateKey rebuiltPrivate = ECCUtil.rebuildPrivateKey(privateKeyBase64);

        assertArrayEquals(pair.getPublic().getEncoded(), rebuiltPublic.getEncoded());
        assertArrayEquals(pair.getPrivate().getEncoded(), rebuiltPrivate.getEncoded());
    }

    @Test
    void encrypt_whenPublicKeyNull_shouldThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> ECCUtil.encrypt("p", null));
    }
}
