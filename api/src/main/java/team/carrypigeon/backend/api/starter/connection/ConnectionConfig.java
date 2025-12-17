package team.carrypigeon.backend.api.starter.connection;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 连接配置类，用于存储连接的配置信息
 * */
@Data
@Configuration
@ConfigurationProperties(prefix = "connection")
public class ConnectionConfig {
    // 连接端口
    private int port;

    /**
     * ECC 公钥（Base64 编码的 X.509 格式），可选。
     * 如未配置，将在服务启动时生成新的密钥对。
     */
    private String eccPublicKey;

    /**
     * ECC 私钥（Base64 编码的 PKCS#8 格式），可选。
     * 如未配置，将在服务启动时生成新的密钥对。
     */
    private String eccPrivateKey;
}
