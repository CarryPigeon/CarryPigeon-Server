package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;

import java.util.Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;

/** 根据生产环境的 SMTP 参数创建邮件客户端，供发送器和连接检查共用。 */
@Configuration(proxyBeanMethods = false)
@EnvironmentComponent(profiles = PROD)
public class MailServiceConfiguration {

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
}
