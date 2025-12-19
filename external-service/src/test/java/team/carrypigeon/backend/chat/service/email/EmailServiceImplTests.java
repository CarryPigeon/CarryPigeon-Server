package team.carrypigeon.backend.chat.service.email;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceImplTests {

    @Test
    void sendEmail_disabled_shouldSkip() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(sender);
        ReflectionTestUtils.setField(service, "isEnable", false);

        service.sendEmail("to@example.com", "subject", "content");

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_enabledWithoutFrom_shouldThrow() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(sender);
        ReflectionTestUtils.setField(service, "isEnable", true);
        ReflectionTestUtils.setField(service, "from", "");

        assertThrows(IllegalStateException.class, () ->
                service.sendEmail("to@example.com", "subject", "content"));

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_enabledWithoutTo_shouldThrow() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(sender);
        ReflectionTestUtils.setField(service, "isEnable", true);
        ReflectionTestUtils.setField(service, "from", "from@example.com");

        assertThrows(IllegalArgumentException.class, () ->
                service.sendEmail(" ", "subject", "content"));

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_enabled_shouldSend() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(sender);
        ReflectionTestUtils.setField(service, "isEnable", true);
        ReflectionTestUtils.setField(service, "from", "from@example.com");

        service.sendEmail("to@example.com", "subject", "content");

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("from@example.com", msg.getFrom());
        assertEquals("to@example.com", msg.getTo()[0]);
        assertEquals("subject", msg.getSubject());
        assertEquals("content", msg.getText());
    }
}

