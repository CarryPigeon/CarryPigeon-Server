package team.carrypigeon.backend.connection.security;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.starter.connection.ConnectionConfig;
import team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil;

import java.security.KeyPair;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EccServerKeyHolderTests {

    @Test
    void whenConfiguredKeysProvided_shouldLoadFromConfiguration() {
        KeyPair pair = ECCUtil.generateEccKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        ConnectionConfig config = new ConnectionConfig();
        config.setEccPublicKey(publicKeyBase64);
        config.setEccPrivateKey(privateKeyBase64);

        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        assertArrayEquals(pair.getPublic().getEncoded(), holder.getPublicKey().getEncoded());
        assertArrayEquals(pair.getPrivate().getEncoded(), holder.getPrivateKey().getEncoded());
    }

    @Test
    void whenInvalidConfiguredKeysProvided_shouldGenerateNewKeyPair() {
        ConnectionConfig config = new ConnectionConfig();
        config.setEccPublicKey("bad");
        config.setEccPrivateKey("bad");

        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        assertNotNull(holder.getPublicKey());
        assertNotNull(holder.getPrivateKey());
    }

    @Test
    void whenNoConfiguredKeysProvided_shouldGenerateNewKeyPair() {
        ConnectionConfig config = new ConnectionConfig();
        EccServerKeyHolder holder = new EccServerKeyHolder(config);
        assertNotNull(holder.getPublicKey());
        assertNotNull(holder.getPrivateKey());
    }
}

