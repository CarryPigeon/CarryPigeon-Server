package team.carrypigeon.backend.infrastructure.service.mail.impl.smtp;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import team.carrypigeon.backend.infrastructure.service.mail.api.exception.MailServiceException;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.api.service.MailSenderService;
import team.carrypigeon.backend.infrastructure.service.mail.impl.config.MailServiceProperties;

/**
 * SMTP 邮件发送服务。
 * 职责：基于 Spring Mail 实现 mail-api 定义的纯文本邮件发送契约。
 * 边界：SMTP 访问细节封装在 impl 内部，不向上层泄露。
 */
public class SmtpMailSenderService implements MailSenderService {

    private final JavaMailSender mailSender;
    private final MailServiceProperties properties;

    public SmtpMailSenderService(JavaMailSender mailSender, MailServiceProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /**
     * 发送纯文本邮件。
     * 输入：收件人、标题和正文。
     * 失败：SMTP 发送失败时抛出稳定的 MailServiceException。
     *
     * @param command 邮件发送命令
     */
    @Override
    public void send(MailSendCommand command) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromAddress());
        message.setTo(command.to().trim());
        message.setSubject(command.subject().trim());
        message.setText(command.text());
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new MailServiceException("failed to send email", exception);
        }
    }
}
