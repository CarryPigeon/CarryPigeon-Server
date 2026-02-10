package team.carrypigeon.backend.chat.domain.cmp.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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

    /**
     * 执行节点处理逻辑并更新上下文。
     *
     * @param session 当前请求会话（仅用于节点签名）
     * @param context LiteFlow 上下文，读取邮箱并执行验证码发送链路
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        String email = requireContext(context, CPNodeValueKeyExtraConstants.EMAIL);

        if (!mailEnabled) {
            fail(CPProblem.of(CPProblemReason.EMAIL_SERVICE_DISABLED, "email service disabled"));
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
            fail(CPProblem.of(CPProblemReason.EMAIL_SEND_FAILED, "send email error"));
        }
    }

    /**
     * 生成 6 位数字验证码。
     *
     * @return 六位数字字符串验证码
     */
    private String generate6DigitCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    /**
     * 规范化验证码过期秒数配置。
     *
     * @param configured 配置中的验证码过期秒数
     * @return 合法的过期秒数（非法值回退到 300 秒）
     */
    private int normalizeExpireSeconds(int configured) {
        if (configured <= 0) {
            return 300;
        }
        return configured;
    }

    /**
     * 构建验证码邮件正文内容。
     *
     * @param code 验证码字符串
     * @param expireSeconds 验证码有效期（秒）
     * @return 发送给用户的邮件正文内容
     */
    private String buildContent(String code, int expireSeconds) {
        int minutes = Math.max(1, expireSeconds / 60);
        return "您的验证码为：" + code + "，" + minutes + " 分钟内有效。";
    }
}
