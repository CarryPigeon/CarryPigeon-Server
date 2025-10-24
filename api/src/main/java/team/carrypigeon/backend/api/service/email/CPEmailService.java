package team.carrypigeon.backend.api.service.email;

/**
 * 邮件发送接口
 * */
public interface CPEmailService {
    /**
     * 发送邮件
     * @param to 收件人
     * @param subject 邮件主题
     * @param text 邮件内容
     */
    void sendEmail(String to, String subject, String text);
}