package team.carrypigeon.backend.infrastructure.service.cache.api.service;

import java.time.Duration;

/**
 * 有限次数的一次性验证码挑战存储；键的业务身份、验证码生成和投递由调用方决定。
 * 挑战替换、尝试扣减、消费和作废必须由共享存储原子完成，禁止读取后无条件覆盖。
 */
public interface CodeChallengeStore {

    /** 用新挑战整体替换同键的旧挑战；attempts 必须为正数，ttl 必须为正毫秒数。 */
    void replace(String key, String challengeId, String code, int attempts, Duration ttl);

    /** 校验当前挑战并消费或扣减尝试；并发替换后不得修改先前读取的旧挑战以外的记录。 */
    Result verify(String key, String code);

    /** 仅删除指定挑战，旧投递失败的清理不能删掉后来签发的新挑战。 */
    void discard(String key, String challengeId);

    /** 不存在和已过期无法从已删除的缓存键中区分，统一为 UNAVAILABLE。 */
    enum Result {
        VERIFIED, INVALID, EXHAUSTED, CONSUMED, UNAVAILABLE
    }
}
