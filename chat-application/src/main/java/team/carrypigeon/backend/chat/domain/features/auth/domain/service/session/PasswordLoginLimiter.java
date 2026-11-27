package team.carrypigeon.backend.chat.domain.features.auth.domain.service.session;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;

/**
 * 密码登录的共享限流：IP 每分钟 30 次，账号五分钟内最多 5 次失败或在途校验。
 * 账号名额在密码校验前预占，失败保留至过期，成功只释放自己的名额；不长期锁定账号。
 */
@Component
public class PasswordLoginLimiter {

    private static final int ACCOUNT_LIMIT = 5;
    private static final Duration ACCOUNT_WINDOW = Duration.ofMinutes(5);
    private static final int IP_LIMIT = 30;
    private static final Duration IP_WINDOW = Duration.ofMinutes(1);
    private final CacheService cacheService;

    public PasswordLoginLimiter(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /** 在查询账号和计算密码哈希前限制 IP 尝试数；重复拒绝不延长窗口。 */
    public void acquireIp(String clientIp) {
        try {
            if (cacheService.increment("auth:pwd:ip:" + clientIp, IP_WINDOW) > IP_LIMIT) {
                throw ProblemException.of(AuthProblem.LOGIN_RATE_LIMITED,
                        Map.of("retry_after_seconds", IP_WINDOW.toSeconds()));
            }
        } catch (CacheServiceException exception) {
            throw ProblemException.of(AuthProblem.LOGIN_LIMIT_UNAVAILABLE, exception);
        }
    }

    /**
     * 占用一个校验名额。已存在账号使用数值 ID 身份，避免数据库大小写或重音等价的用户名绕过配额；
     * 不存在账号使用规范化用户名身份，同样在查询后走这一步，不泄露不同的限流错误结构。
     */
    public Attempt reserveAccount(String accountIdentity) {
        // 编码业务身份只为固定键的分段边界，不借用 refresh token 的摘要能力。
        String keyPrefix = "auth:pwd:account:" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(accountIdentity.getBytes(StandardCharsets.UTF_8)) + ":";
        String token = UUID.randomUUID().toString();
        try {
            // 五个独立的 SET NX 名额在多实例间竞争，先到期的失败名额会自然恢复。
            for (int slot = 0; slot < ACCOUNT_LIMIT; slot++) {
                String key = keyPrefix + slot;
                if (cacheService.setIfAbsent(key, token, ACCOUNT_WINDOW)) {
                    return new Attempt(key, token);
                }
            }
            throw ProblemException.of(AuthProblem.LOGIN_RATE_LIMITED,
                    Map.of("retry_after_seconds", ACCOUNT_WINDOW.toSeconds()));
        } catch (CacheServiceException exception) {
            throw ProblemException.of(AuthProblem.LOGIN_LIMIT_UNAVAILABLE, exception);
        }
    }

    /** 成功验证密码后释放自己的名额；比较 token 可避免旧请求误删名额过期后的新请求。 */
    public void releaseSuccessful(Attempt attempt) {
        try {
            cacheService.consumeIfEquals(attempt.key(), attempt.token());
        } catch (CacheServiceException exception) {
            throw ProblemException.of(AuthProblem.LOGIN_LIMIT_UNAVAILABLE, exception);
        }
    }

    /** 本次校验占用的缓存名额，token 只用于安全释放，不作为用户凭证。 */
    public record Attempt(String key, String token) {
    }
}
