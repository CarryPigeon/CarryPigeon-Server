package team.carrypigeon.backend.chat.domain.support.dao;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.service.email.CPEmailService;

/**
 * Test double for {@link CPEmailService}.
 * Captures the latest send request for assertions.
 */
@Component
public class InMemoryEmailService implements CPEmailService {

    private volatile String lastTo;
    private volatile String lastSubject;
    private volatile String lastText;

    /**
     * 测试邮件服务辅助方法。
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param text 邮件正文
     */
    @Override
    public void sendEmail(String to, String subject, String text) {
        this.lastTo = to;
        this.lastSubject = subject;
        this.lastText = text;
    }

    /**
     * 测试邮件服务辅助方法。
     *
     * @return 最近一次发送记录中的对应字段值
     */
    public String getLastTo() {
        return lastTo;
    }

    /**
     * 测试邮件服务辅助方法。
     *
     * @return 最近一次发送记录中的对应字段值
     */
    public String getLastSubject() {
        return lastSubject;
    }

    /**
     * 测试邮件服务辅助方法。
     *
     * @return 最近一次发送记录中的对应字段值
     */
    public String getLastText() {
        return lastText;
    }

    /**
     * 测试邮件服务辅助方法。
     */
    public void clear() {
        this.lastTo = null;
        this.lastSubject = null;
        this.lastText = null;
    }
}

