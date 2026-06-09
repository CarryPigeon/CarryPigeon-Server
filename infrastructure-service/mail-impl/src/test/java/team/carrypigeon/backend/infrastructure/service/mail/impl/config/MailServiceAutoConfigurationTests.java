package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证邮件自动配置的装配边界。
 * 职责：确保 mail-impl 只在启用条件满足时注册邮件服务相关 Bean。
 * 边界：不连接真实 SMTP，只验证自动配置的上下文装配行为。
 */
@Tag("contract")
class MailServiceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    MailServiceAutoConfiguration.class
            ));

    /**
     * 测试启用邮件服务时的自动配置。
     * 输入：启用开关、默认发件地址和 Spring Mail 发送器。
     * 期望：成功装配邮件发送、健康检查和初始化检查 Bean。
     */
    @Test
    void autoConfiguration_enabled_registersMailBeans() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.mail.enabled=true",
                        "cp.infrastructure.service.mail.from-address=noreply@example.com"
                )
                .withBean(JavaMailSenderImpl.class, () -> mock(JavaMailSenderImpl.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(MailSenderService.class);
                    assertThat(context).hasSingleBean(MailHealthService.class);
                    assertThat(context).hasSingleBean(InitializationCheck.class);
                });
    }

    /**
     * 测试禁用邮件服务时的自动配置。
     * 输入：禁用开关和 Spring Mail 发送器。
     * 期望：mail-impl 不注册邮件服务相关 Bean。
     */
    @Test
    void autoConfiguration_disabled_skipsMailBeans() {
        contextRunner
                .withPropertyValues("cp.infrastructure.service.mail.enabled=false")
                .withBean(JavaMailSenderImpl.class, () -> mock(JavaMailSenderImpl.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MailSenderService.class);
                    assertThat(context).doesNotHaveBean(MailHealthService.class);
                    assertThat(context).doesNotHaveBean(InitializationCheck.class);
                });
    }
}
