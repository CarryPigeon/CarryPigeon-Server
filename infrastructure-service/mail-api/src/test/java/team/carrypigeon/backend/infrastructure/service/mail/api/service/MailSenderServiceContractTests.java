package team.carrypigeon.backend.infrastructure.service.mail.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.mail.api.model.MailSendCommand;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * MailSenderService 契约测试。
 * 职责：验证 mail-api 发送服务抽象只暴露纯文本邮件命令边界。
 * 边界：不连接真实 SMTP，也不验证具体邮件实现。
 */
@Tag("contract")
class MailSenderServiceContractTests {

    /**
     * 验证发送服务实现方接收完整邮件发送命令。
     */
    @Test
    @DisplayName("send command passes complete mail command")
    void send_command_passesCompleteMailCommand() {
        RecordingMailSenderService service = new RecordingMailSenderService();
        MailSendCommand command = new MailSendCommand("user@example.com", "Verify", "Code: 123456");

        service.send(command);

        assertSame(command, service.lastCommand);
    }

    /**
     * RecordingMailSenderService 测试替身。
     * 职责：记录发送命令，使测试只验证 mail-api 抽象边界。
     */
    private static class RecordingMailSenderService implements MailSenderService {
        private MailSendCommand lastCommand;

        @Override
        public void send(MailSendCommand command) {
            this.lastCommand = command;
        }
    }
}
