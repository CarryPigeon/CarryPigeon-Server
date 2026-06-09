package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件服务配置。
 * 职责：控制 mail-impl 是否装配以及默认发件地址。
 * 边界：SMTP 主机、端口和认证信息仍由 Spring 标准 `spring.mail.*` 配置承接。
 *
 * @param enabled 是否启用邮件服务实现
 * @param fromAddress 默认发件邮箱地址
 */
@ConfigurationProperties(prefix = "cp.infrastructure.service.mail")
public record MailServiceProperties(boolean enabled, String fromAddress) {

    public MailServiceProperties {
        if (fromAddress == null) {
            fromAddress = "";
        }
        if (enabled && fromAddress.isBlank()) {
            throw new IllegalArgumentException("cp.infrastructure.service.mail.from-address must not be blank when mail is enabled");
        }
    }

    public MailServiceProperties() {
        this(false, "");
    }
}
