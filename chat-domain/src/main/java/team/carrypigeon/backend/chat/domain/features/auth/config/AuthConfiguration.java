package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 鉴权功能配置入口。
 * 职责：启用 auth feature 内部的稳定配置绑定。
 * 边界：不创建外部服务实现 Bean，不承载鉴权业务规则。
 */
@Configuration
@EnableConfigurationProperties(AuthJwtProperties.class)
public class AuthConfiguration {
}
