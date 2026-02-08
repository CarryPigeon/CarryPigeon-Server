package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowConnectionInfo;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractCheckerNode;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 邮箱验证码发送限流校验节点。
 * <p>
 * 默认以“硬校验”方式工作：超过阈值直接返回错误响应并中断链路。
 * 当前策略：
 * <ul>
 *     <li>维度：remoteIp + email</li>
 *     <li>短窗口：{@link #SHORT_WINDOW_SECONDS} 秒内最多 {@link #MAX_REQUESTS_PER_SHORT_WINDOW} 次</li>
 *     <li>日窗口：{@link #DAILY_WINDOW_SECONDS} 秒内最多 {@link #MAX_REQUESTS_PER_DAY} 次</li>
 * </ul>
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("EmailRateLimitChecker")
public class EmailRateLimitChecker extends AbstractCheckerNode {

    /** 短窗口大小（秒），60s 内限流。 */
    private static final int SHORT_WINDOW_SECONDS = 60;

    /** 一天窗口大小（秒），用于总次数限制。 */
    private static final int DAILY_WINDOW_SECONDS = 24 * 60 * 60;

    /** 单个 IP + 邮箱在短窗口内允许的最大请求次数。 */
    private static final int MAX_REQUESTS_PER_SHORT_WINDOW = 5;

    /** 单个 IP + 邮箱在一天内允许的最大请求次数。 */
    private static final int MAX_REQUESTS_PER_DAY = 30;

    private final CPCache cache;

    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        // 必填参数：邮箱
        String email = requireContext(context, CPNodeValueKeyExtraConstants.EMAIL);

        // 从上下文中读取连接信息（由分发器在链路开始时写入）
        CPFlowConnectionInfo connectionInfo = context.getConnectionInfo();
        String remoteIp = connectionInfo != null ? connectionInfo.getRemoteIp() : null;
        String remoteAddress = connectionInfo != null ? connectionInfo.getRemoteAddress() : null;
        if (remoteIp == null || remoteIp.isEmpty()) {
            // 兜底：在测试或非 Netty 场景中可能没有 IP 信息
            remoteIp = "unknown";
        }

        // 60 秒窗口限流
        String shortKey = buildShortWindowKey(remoteIp, email);
        int shortCount = increaseAndGet(shortKey, SHORT_WINDOW_SECONDS);
        if (shortCount > MAX_REQUESTS_PER_SHORT_WINDOW) {
            log.info("EmailRateLimitChecker hard fail: short window rate limited, " +
                            "remoteIp={}, remoteAddress={}, email={}, count={}, windowSeconds={}",
                    remoteIp, remoteAddress, email, shortCount, SHORT_WINDOW_SECONDS);
            fail(CPProblem.of(429, "rate_limited", "too many email requests"));
        }

        // 按天总次数限流
        String dailyKey = buildDailyWindowKey(remoteIp, email);
        int dailyCount = increaseAndGet(dailyKey, DAILY_WINDOW_SECONDS);
        if (dailyCount > MAX_REQUESTS_PER_DAY) {
            log.info("EmailRateLimitChecker hard fail: daily window rate limited, " +
                            "remoteIp={}, remoteAddress={}, email={}, count={}, windowSeconds={}",
                    remoteIp, remoteAddress, email, dailyCount, DAILY_WINDOW_SECONDS);
            fail(CPProblem.of(429, "rate_limited", "too many email requests"));
        }

        log.debug("EmailRateLimitChecker pass: remoteIp={}, email={}, shortCount={}, dailyCount={}",
                remoteIp, email, shortCount, dailyCount);
    }

    /**
     * 生成 60s 窗口限流用缓存 key。
     */
    private String buildShortWindowKey(String remoteIp, String email) {
        String safeIp = remoteIp == null ? "null" : remoteIp;
        String safeEmail = email == null ? "null" : email;
        return "email_rate:ip_email:" + safeIp + ":" + safeEmail;
    }

    /**
     * 生成按天限流用缓存 key。
     */
    private String buildDailyWindowKey(String remoteIp, String email) {
        String safeIp = remoteIp == null ? "null" : remoteIp;
        String safeEmail = email == null ? "null" : email;
        return "email_rate_day:ip_email:" + safeIp + ":" + safeEmail;
    }

    /**
     * 简单的“自增并返回”实现，仅用于限流场景，对强一致性无特殊要求。
     */
    private int increaseAndGet(String key, int windowSeconds) {
        String current = cache.get(key);
        int next = 1;
        if (current != null) {
            try {
                int parsed = Integer.parseInt(current);
                if (parsed > 0) {
                    next = parsed + 1;
                }
            } catch (NumberFormatException e) {
                // 非法值时回退为 1，并覆盖
                log.warn("EmailRateLimitChecker found non-numeric value in cache, key={}, value={}", key, current);
            }
        }
        cache.set(key, String.valueOf(next), windowSeconds);
        return next;
    }
}
