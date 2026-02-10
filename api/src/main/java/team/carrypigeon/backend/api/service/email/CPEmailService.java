package team.carrypigeon.backend.api.service.email;

/**
 * 邮件服务接口。
 */
public interface CPEmailService {

    /**
     * 发送邮件。
     *
     * @param to 收件人邮箱。
     * @param subject 邮件主题。
     * @param text 邮件正文。
     */
    void sendEmail(String to, String subject, String text);
}
