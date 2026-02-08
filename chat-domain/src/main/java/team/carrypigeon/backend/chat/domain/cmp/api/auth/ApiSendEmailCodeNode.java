package team.carrypigeon.backend.chat.domain.cmp.api.auth;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.api.service.email.CPEmailService;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.SendEmailCodeRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;

/**
 * Send a 6-digit email code for login.
 * <p>
 * Route: {@code POST /api/auth/email_codes} (public)
 * <p>
 * Input: {@link ApiFlowKeys#REQUEST} = {@link SendEmailCodeRequest}
 * Output: none (HTTP 204 is handled by controller)
 * <p>
 * Storage: code is stored in {@link CPCache} with a TTL and deleted when consumed by {@code /api/auth/tokens}.
 */
@Slf4j
@LiteflowComponent("ApiSendEmailCode")
@RequiredArgsConstructor
public class ApiSendEmailCodeNode extends CPNodeComponent {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SHORT_WINDOW_SECONDS = 60;
    private static final int DAILY_WINDOW_SECONDS = 24 * 60 * 60;
    private static final int MAX_REQUESTS_PER_SHORT_WINDOW = 5;
    private static final int MAX_REQUESTS_PER_DAY = 30;

    private final CPCache cache;
    private final CPEmailService emailService;

    @Value("${spring.mail.enable:false}")
    private boolean mailEnabled;

    @Value("${carrypigeon.email.code-expire-seconds:300}")
    private int codeExpireSeconds;

    @Value("${carrypigeon.email.code-subject:CarryPigeon 验证码}")
    private String subject;

    @Override
    protected void process(CPFlowContext context) throws Exception {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof SendEmailCodeRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        if (!mailEnabled) {
            throw new CPProblemException(CPProblem.of(500, "internal_error", "email service disabled"));
        }
        enforceRateLimit(req.email());
        String code = generate6DigitCode();
        int expireSeconds = normalizeExpireSeconds(codeExpireSeconds);
        cache.set(emailCodeKey(req.email()), code, expireSeconds);
        emailService.sendEmail(req.email(), subject, buildContent(code, expireSeconds));
        log.debug("ApiSendEmailCode success, email={}, expireSeconds={}", req.email(), expireSeconds);
    }

    private String emailCodeKey(String email) {
        return email + ":code";
    }

    private void enforceRateLimit(String email) {
        String remoteIp = resolveRemoteIp();

        String shortKey = "api:email_code:short:" + remoteIp + ":" + email;
        long shortCount = cache.increment(shortKey, 1L, SHORT_WINDOW_SECONDS);
        if (shortCount > MAX_REQUESTS_PER_SHORT_WINDOW) {
            throw new CPProblemException(CPProblem.of(429, "rate_limited", "too many email requests"));
        }

        String dailyKey = "api:email_code:day:" + remoteIp + ":" + email;
        long dailyCount = cache.increment(dailyKey, 1L, DAILY_WINDOW_SECONDS);
        if (dailyCount > MAX_REQUESTS_PER_DAY) {
            throw new CPProblemException(CPProblem.of(429, "rate_limited", "too many email requests"));
        }
    }

    private String resolveRemoteIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            HttpServletRequest request = attrs.getRequest();
            if (request == null) {
                return "unknown";
            }
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            String forwarded = request.getHeader("Forwarded");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.trim();
            }
            String remoteAddr = request.getRemoteAddr();
            return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
        } catch (Exception ignored) {
            return "unknown";
        }
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

    private String generate6DigitCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
