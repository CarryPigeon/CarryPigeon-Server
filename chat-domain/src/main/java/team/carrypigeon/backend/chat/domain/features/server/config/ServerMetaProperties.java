package team.carrypigeon.backend.chat.domain.features.server.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务端的原始信息，给发现使用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "cp.server.meta")
public class ServerMetaProperties {
    private String id;
    private String name;
    private String description;
}
