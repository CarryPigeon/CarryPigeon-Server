package team.carrypigeon.backend.chat.domain.config.server;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import team.carrypigeon.backend.chat.domain.shared.domain.server.ServerIdentityProvider;

/**
 * 服务端身份配置。
 * 职责：提供服务发现与实时通道握手使用的最小 serverId 来源。
 * 边界：只承载稳定配置，不承载公开源信息或多服务端协商逻辑。
 * 约束：不可变记录只能构造器绑定，因此由 `@ConfigurationPropertiesScan` 启用，而不是标 `@Component`。
 *
 * @param id 当前服务端稳定标识
 */
@ConfigurationProperties(prefix = "cp.chat.server")
public record ServerIdentityProperties(String id) implements ServerIdentityProvider {

    public ServerIdentityProperties {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("id must be a stable UUID string", exception);
        }
    }
}
