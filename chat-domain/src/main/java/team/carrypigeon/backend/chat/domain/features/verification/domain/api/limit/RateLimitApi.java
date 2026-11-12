package team.carrypigeon.backend.chat.domain.features.verification.domain.api.limit;

public interface RateLimitApi {

    /**
     * @param key    限流维度标识，如 "sms:mobile:138xxx"
     * @param seconds 窗口秒数
     */
    boolean tryAcquire(String key, long seconds);

    /**
     * 固定窗口计数：返回窗口内累计次数（包含本次）。
     * @param key    限流维度标识，如 "sms:mobile:138xxx"
     * @param seconds 窗口秒数
     * */
    long count(String key, long seconds);
}
