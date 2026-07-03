package team.carrypigeon.backend.infrastructure.service.mail.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MailSendCommand 单元测试。
 * 职责：验证 mail-api 对纯文本邮件发送命令的最小参数约束。
 * 边界：不验证 SMTP、模板或邮件发送实现行为。
 */
@Tag("unit")
class MailSendCommandTests {

    /**
     * 验证合法命令会保留收件人、标题和正文。
     */
    @Test
    @DisplayName("constructor valid command keeps mail fields")
    void constructor_validCommand_keepsMailFields() {
        MailSendCommand command = new MailSendCommand("user@example.com", "Verify", "Code: 123456");

        assertEquals("user@example.com", command.to());
        assertEquals("Verify", command.subject());
        assertEquals("Code: 123456", command.text());
    }

    /**
     * 验证空白收件人会在 API 模型层被拒绝。
     */
    @Test
    @DisplayName("constructor blank recipient throws illegal argument")
    void constructor_blankRecipient_throwsIllegalArgument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MailSendCommand(" ", "Verify", "Code: 123456")
        );

        assertEquals("mail recipient must not be blank", exception.getMessage());
    }

    /**
     * 验证空白标题会在 API 模型层被拒绝。
     */
    @Test
    @DisplayName("constructor blank subject throws illegal argument")
    void constructor_blankSubject_throwsIllegalArgument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MailSendCommand("user@example.com", "", "Code: 123456")
        );

        assertEquals("mail subject must not be blank", exception.getMessage());
    }

    /**
     * 验证空白正文会在 API 模型层被拒绝。
     */
    @Test
    @DisplayName("constructor blank text throws illegal argument")
    void constructor_blankText_throwsIllegalArgument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new MailSendCommand("user@example.com", "Verify", null)
        );

        assertEquals("mail text must not be blank", exception.getMessage());
    }
}
