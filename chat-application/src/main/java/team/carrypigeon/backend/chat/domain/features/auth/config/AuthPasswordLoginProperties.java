package team.carrypigeon.backend.chat.domain.features.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户名密码登录配置。
 * 职责：承载用户名密码登录入口的运行时开关。
 * 边界：只控制 `POST /api/auth/login` 是否允许签发 token，不影响邮箱验证码、refresh 或 Bearer 鉴权链路。
 *
 * @param enabled 是否启用用户名密码登录
 */
@ConfigurationProperties(prefix = "cp.chat.auth.password-login")
public record AuthPasswordLoginProperties(Boolean enabled) {

    public AuthPasswordLoginProperties {
        if (enabled == null) {
            enabled = true;
        }
    }

    /**
     * 判断用户名密码登录是否启用。
     *
     * @return 启用时返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }
}
