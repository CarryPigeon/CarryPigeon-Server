package team.carrypigeon.backend.infrastructure.service.mail.impl.smtp;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealth;
import team.carrypigeon.backend.infrastructure.service.mail.api.health.MailHealthService;

/**
 * SMTP 邮件健康检查实现。
 * 职责：通过 SMTP 连接测试验证邮件服务是否可用。
 * 边界：SMTP 连接细节停留在 impl 内部。
 */
public class SmtpMailHealthService implements MailHealthService {

    private final JavaMailSenderImpl mailSender;

    public SmtpMailHealthService(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 执行 SMTP 健康检查。
     *
     * @return 邮件服务健康状态
     */
    @Override
    public MailHealth check() {
        try {
            mailSender.testConnection();
            return new MailHealth(true, "mail service is available");
        } catch (Exception exception) {
            return new MailHealth(false, "mail service is unavailable: " + exception.getMessage());
        }
    }
}
