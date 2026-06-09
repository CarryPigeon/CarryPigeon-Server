package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.smtp.SmtpMailHealthService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.smtp.SmtpMailSenderService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.startup.MailInitializationCheck;

/**
 * 邮件服务自动配置。
 * 职责：在 mail-impl 内装配 SMTP 邮件发送与健康检查能力。
 * 边界：只创建具体实现 Bean，不在 API 模块中定义 Spring 装配逻辑。
 */
@AutoConfiguration
@EnableConfigurationProperties(MailServiceProperties.class)
@ConditionalOnProperty(prefix = "cp.infrastructure.service.mail", name = "enabled", havingValue = "true")
public class MailServiceAutoConfiguration {

    /**
     * 创建邮件发送服务。
     *
     * @param mailSender Spring Mail 发送器
     * @param properties 邮件服务配置
     * @return 邮件发送服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MailSenderService mailSenderService(JavaMailSenderImpl mailSender, MailServiceProperties properties) {
        return new SmtpMailSenderService(mailSender, properties);
    }

    /**
     * 创建邮件健康检查服务。
     *
     * @param mailSender Spring Mail 发送器
     * @return 邮件健康检查服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MailHealthService mailHealthService(JavaMailSenderImpl mailSender) {
        return new SmtpMailHealthService(mailSender);
    }

    /**
     * 创建邮件初始化检查。
     *
     * @param mailHealthService 邮件健康检查服务
     * @return 共享初始化检查契约下的邮件检查
     */
    @Bean
    @ConditionalOnMissingBean(name = "mailInitializationCheck")
    public InitializationCheck mailInitializationCheck(MailHealthService mailHealthService) {
        return new MailInitializationCheck(mailHealthService);
    }
}
