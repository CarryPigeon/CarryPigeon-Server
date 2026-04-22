package team.carrypigeon.backend.chat.domain.features.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 服务端身份配置。
 * 职责：提供当前服务实例在消息模型中真实使用的最小 serverId 来源。
 * 边界：这里只承载稳定配置，不承载公开源信息或多服务端协商逻辑。
 *
 * @param id 当前服务端稳定标识
 */
@ConfigurationProperties(prefix = "cp.chat.server")
public record ServerIdentityProperties(String id) {

    public ServerIdentityProperties {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    public ServerIdentityProperties() {
        this("carrypigeon-local");
    }
}
