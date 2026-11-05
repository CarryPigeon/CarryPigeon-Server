package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import java.util.Properties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.smtp.SmtpMailHealthService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.smtp.SmtpMailSenderService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.startup.MailInitializationChecker;

/**
 * 邮件服务自动配置。
 * 职责：在 mail-impl 内装配 SMTP 邮件发送与健康检查能力。
 * 边界：只创建具体实现 Bean，不在 API 模块中定义 Spring 装配逻辑。
 */
@AutoConfiguration
@EnableConfigurationProperties(SmtpMailProperties.class)
@ConditionalOnProperty(prefix = "cp.service.mail.smtp", name = "enabled", havingValue = "true")
public class MailServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JavaMailSenderImpl javaMailSender(SmtpMailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        sender.setUsername(properties.username());
        sender.setPassword(properties.password());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(properties.auth()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(properties.connectionTimeout()));
        props.put("mail.smtp.timeout", String.valueOf(properties.timeout()));
        props.put("mail.smtp.writetimeout", String.valueOf(properties.writeTimeout()));
        props.put("mail.smtp.starttls.enable", String.valueOf(properties.starttlsEnable()));

        return sender;
    }

    @Bean
    @ConditionalOnMissingBean
    public MailSenderService mailSenderService(JavaMailSenderImpl mailSender, SmtpMailProperties properties) {
        return new SmtpMailSenderService(mailSender, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public MailHealthService mailHealthService(JavaMailSenderImpl mailSender) {
        return new SmtpMailHealthService(mailSender);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mailInitializationCheck")
    public InitializationChecker mailInitializationCheck(MailHealthService mailHealthService) {
        return new MailInitializationChecker(mailHealthService);
    }
}
