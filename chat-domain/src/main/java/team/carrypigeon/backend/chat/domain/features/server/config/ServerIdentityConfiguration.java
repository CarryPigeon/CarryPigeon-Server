package team.carrypigeon.backend.chat.domain.features.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 服务端身份配置装配。
 * 职责：注册消息模型真实使用的 serverId 配置来源。
 * 边界：不承载 realtime 运行时装配与业务逻辑。
 */
@Configuration
@EnableConfigurationProperties(ServerIdentityProperties.class)
public class ServerIdentityConfiguration {
}
