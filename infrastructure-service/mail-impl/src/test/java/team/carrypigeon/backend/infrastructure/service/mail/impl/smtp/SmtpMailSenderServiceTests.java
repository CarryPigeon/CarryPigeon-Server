package team.carrypigeon.backend.infrastructure.service.mail.impl.smtp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import team.carrypigeon.backend.infrastructure.service.mail.api.exception.MailServiceException;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;
import team.carrypigeon.backend.infrastructure.service.mail.impl.config.MailServiceProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SmtpMailSenderService 契约测试。
 * 职责：验证 SMTP 邮件发送实现会按稳定契约拼装纯文本邮件。
 * 边界：不连接真实 SMTP，只验证与 Spring Mail 的交互语义。
 */
@Tag("contract")
class SmtpMailSenderServiceTests {

    /**
     * 验证发送邮件时会携带默认发件地址、收件人、标题和正文。
     */
    @Test
    @DisplayName("send valid command delegates to spring mail sender")
    void send_validCommand_delegatesToSpringMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpMailSenderService service = new SmtpMailSenderService(
                mailSender,
                new MailServiceProperties(true, "noreply@example.com")
        );

        service.send(new MailSendCommand("user@example.com", "subject", "body"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("noreply@example.com", captor.getValue().getFrom());
        assertEquals("user@example.com", captor.getValue().getTo()[0]);
        assertEquals("subject", captor.getValue().getSubject());
        assertEquals("body", captor.getValue().getText());
    }

    /**
     * 验证底层发送失败时会收口为稳定的 MailServiceException。
     */
    @Test
    @DisplayName("send spring mail failure throws mail service exception")
    void send_springMailFailure_throwsMailServiceException() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("boom")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        SmtpMailSenderService service = new SmtpMailSenderService(
                mailSender,
                new MailServiceProperties(true, "noreply@example.com")
        );

        MailServiceException exception = assertThrows(
                MailServiceException.class,
                () -> service.send(new MailSendCommand("user@example.com", "subject", "body"))
        );

        assertEquals("failed to send email", exception.getMessage());
    }
}
