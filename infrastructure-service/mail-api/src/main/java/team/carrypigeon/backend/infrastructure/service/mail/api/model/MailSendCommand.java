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

    private static final int MAX_RECIPIENT_LENGTH = 320;
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_TEXT_LENGTH = 100_000;

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
        to = to.trim();
        subject = subject.trim();
        text = text.trim();
        validateRecipient(to);
        if (subject.length() > MAX_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("mail subject must not exceed 255 characters");
        }
        if (containsLineBreak(subject)) {
            throw new IllegalArgumentException("mail subject must not contain line breaks");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("mail text must not exceed 100000 characters");
        }
    }

    private static void validateRecipient(String recipient) {
        if (recipient.length() > MAX_RECIPIENT_LENGTH) {
            throw new IllegalArgumentException("mail recipient must not exceed 320 characters");
        }
        if (containsLineBreak(recipient)) {
            throw new IllegalArgumentException("mail recipient must not contain line breaks");
        }
        int separator = recipient.indexOf('@');
        if (separator <= 0 || separator != recipient.lastIndexOf('@') || separator == recipient.length() - 1
                || recipient.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("mail recipient must be a basic email address");
        }
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }
}
