package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthPasswordLoginPolicy;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenSettings;

/**
 * 鉴权功能配置入口。
 * 职责：启用 auth feature 内部的稳定配置绑定。
 * 边界：不创建外部服务实现 Bean，不承载鉴权业务规则。
 */
@Configuration
@EnableConfigurationProperties({AuthJwtProperties.class, AuthPasswordLoginProperties.class})
public class AuthConfiguration {

    /**
     * 创建领域侧 token 签发设置。
     * 职责：把配置绑定对象转换为领域服务需要的最小时间参数。
     *
     * @param properties JWT 配置绑定对象
     * @return token 签发设置
     */
    @Bean
    public AuthTokenSettings authTokenSettings(AuthJwtProperties properties) {
        return new AuthTokenSettings(properties.accessTokenTtl(), properties.refreshTokenTtl());
    }

    /**
     * 创建用户名密码登录策略。
     * 职责：把配置绑定对象转换为领域会话服务使用的登录策略。
     *
     * @param properties 用户名密码登录配置
     * @return 用户名密码登录策略
     */
    @Bean
    public AuthPasswordLoginPolicy authPasswordLoginPolicy(AuthPasswordLoginProperties properties) {
        return new AuthPasswordLoginPolicy(properties.isEnabled());
    }

}
