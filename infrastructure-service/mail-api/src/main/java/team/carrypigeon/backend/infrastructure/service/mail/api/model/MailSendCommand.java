package team.carrypigeon.backend.infrastructure.service.mail.api.model;

/**
 * 邮件发送命令。
 * 职责：承载发送一封纯文本邮件所需的最小稳定参数。
 * 边界：不暴露 SMTP 配置、模板引擎或厂商专属字段。
 *
 * @param to 收件人邮箱
 * @param subject 邮件标题
 * @param text 邮件正文
 */
public record MailSendCommand(String to, String subject, String text) {

    public MailSendCommand {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("mail recipient must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("mail subject must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("mail text must not be blank");
        }
    }
}
