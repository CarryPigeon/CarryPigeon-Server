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
}
