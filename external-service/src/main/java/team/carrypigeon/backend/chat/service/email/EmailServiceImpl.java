package team.carrypigeon.backend.chat.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.service.email.CPEmailService;

/**
 * 邮件发送实现，基于 Spring 的 JavaMailSender。
 * 使用 @Slf4j 记录发送行为和异常，方便排查邮件相关问题。
 */
@Slf4j
@Service
public class EmailServiceImpl implements CPEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${spring.mail.enable:false}")
    private boolean isEnable;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        if (!isEnable) {
            // 邮件功能关闭时打一个调试日志，方便确认配置原因
            log.debug("email send skipped because spring.mail.enable=false, to={}, subject={}", to, subject);
            return;
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("spring.mail.username is required when spring.mail.enable=true");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("to is required");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
            log.info("email sent successfully, to={}, subject={}", to, subject);
        } catch (MailException e) {
            // 记录失败原因，交给上层根据需要决定是否继续抛出
            log.error("failed to send email, to={}, subject={}", to, subject, e);
            throw e;
        }
    }
}
