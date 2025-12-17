package team.carrypigeon.backend.connection.security;

import cn.hutool.core.codec.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.starter.connection.ConnectionConfig;
import team.carrypigeon.backend.connection.protocol.encryption.ecc.ECCUtil;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 服务端 ECC 密钥对持有者。
 * <p>
 * 当前实现为服务启动时生成一对临时密钥，
 * 客户端需预先内置或通过其他安全渠道获取对应的公钥。
 */
@Component
public class EccServerKeyHolder {

    private static final Logger log = LoggerFactory.getLogger(EccServerKeyHolder.class);

    private final KeyPair keyPair;

    public EccServerKeyHolder(ConnectionConfig connectionConfig) {
        KeyPair pair = null;
        String publicKeyBase64 = connectionConfig.getEccPublicKey();
        String privateKeyBase64 = connectionConfig.getEccPrivateKey();
        if (publicKeyBase64 != null && !publicKeyBase64.isEmpty()
                && privateKeyBase64 != null && !privateKeyBase64.isEmpty()) {
            try {
                PublicKey publicKey = ECCUtil.rebuildPublicKey(publicKeyBase64);
                PrivateKey privateKey = ECCUtil.rebuildPrivateKey(privateKeyBase64);
                pair = new KeyPair(publicKey, privateKey);
                log.info("ECC server key pair loaded from configuration");
            } catch (Exception e) {
                log.warn("failed to rebuild ECC key pair from configuration, will generate new one: {}",
                        e.getMessage(), e);
            }
        }
        if (pair == null) {
            pair = ECCUtil.generateEccKeyPair();
            String pub = Base64.encode(pair.getPublic().getEncoded());
            String pri = Base64.encode(pair.getPrivate().getEncoded());
            log.warn("ECC key pair not configured, generated new in-memory key pair. " +
                    "Please persist these values in configuration if you want a stable key:\n" +
                    "connection.ecc-public-key={}\nconnection.ecc-private-key={}", pub, pri);
        }
        this.keyPair = pair;
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}
