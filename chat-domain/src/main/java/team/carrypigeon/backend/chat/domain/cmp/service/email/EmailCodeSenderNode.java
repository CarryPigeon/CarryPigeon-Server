package team.carrypigeon.backend.chat.domain.cmp.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.service.email.CPEmailService;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

import java.security.SecureRandom;

/**
 * 发送邮箱验证码节点。
 * <p>
 * 入参：Email:String<br/>
 * 出参：写入缓存 key = "{email}:code"，value = 6 位数字字符串<br/>
 * <p>
 * 当邮件发送失败时，会尽量回滚删除已写入的验证码，并返回服务器错误响应。
 */
@Slf4j
@RequiredArgsConstructor
@LiteflowComponent("EmailCodeSender")
public class EmailCodeSenderNode extends CPNodeComponent {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CPCache cache;
    private final CPEmailService emailService;

    @Value("${spring.mail.enable:false}")
    private boolean mailEnabled;

    @Value("${carrypigeon.email.code-expire-seconds:300}")
    private int codeExpireSeconds;

    @Value("${carrypigeon.email.code-subject:CarryPigeon 验证码}")
    private String subject;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String email = requireContext(context, CPNodeValueKeyExtraConstants.EMAIL);

        if (!mailEnabled) {
            fail(CPProblem.of(500, "email_service_disabled", "email service disabled"));
        }

        int expireSeconds = normalizeExpireSeconds(codeExpireSeconds);
        String code = generate6DigitCode();
        String cacheKey = email + ":code";

        try {
            cache.set(cacheKey, code, expireSeconds);
            emailService.sendEmail(email, subject, buildContent(code, expireSeconds));
            log.info("email verification code sent, to={}, expireSeconds={}", email, expireSeconds);
        } catch (Exception e) {
            try {
                cache.delete(cacheKey);
            } catch (Exception deleteEx) {
                log.warn("failed to rollback email code cache, key={}", cacheKey, deleteEx);
            }
            log.error("failed to send email verification code, to={}", email, e);
            fail(CPProblem.of(500, "email_send_failed", "send email error"));
        }
    }

    private String generate6DigitCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private int normalizeExpireSeconds(int configured) {
        if (configured <= 0) {
            return 300;
        }
        return configured;
    }

    private String buildContent(String code, int expireSeconds) {
        int minutes = Math.max(1, expireSeconds / 60);
        return "您的验证码为：" + code + "，" + minutes + " 分钟内有效。";
    }
}
