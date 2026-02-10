package team.carrypigeon.backend.chat.domain.service.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件令牌服务。
 * <p>
 * 负责一次性短期令牌的创建与消费。
 */
@Slf4j
@Service
@AllArgsConstructor
public class FileTokenService {

    private final CPCache cpCache;

    /**
     * 创建一次性文件令牌。
     *
     * @param uid 所属用户 ID。
     * @param op 操作类型。
     * @param fileId 关联文件 ID。
     * @param ttlSec 令牌有效期（秒）。
     * @return 生成的令牌字符串。
     */
    public String createToken(long uid, String op, String fileId, long ttlSec) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = TimeUtil.currentLocalDateTime().plusSeconds(ttlSec);
        long expireMillis = TimeUtil.localDateTimeToMillis(expireAt);
        String encoded = uid + ":" + op + ":" + (fileId == null ? "" : fileId) + ":" + expireMillis;
        cpCache.set(buildCacheKey(token), encoded, (int) ttlSec);
        log.debug("create file token, token={}, uid={}, op={}, fileId={} ttlSec={}",
                token, uid, op, fileId, ttlSec);
        return token;
    }

    /**
     * 消费一次性文件令牌。
     *
     * @param token 令牌字符串。
     * @return 解析后的令牌信息；令牌不存在、过期或格式错误返回 {@code null}。
     */
    public FileToken consume(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String encoded = cpCache.getAndDelete(buildCacheKey(token));
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        String[] parts = encoded.split(":", 4);
        if (parts.length < 4) {
            log.warn("file token parse error, token={}, value={}", token, encoded);
            return null;
        }
        try {
            long uid = Long.parseLong(parts[0]);
            String op = parts[1];
            String fileId = parts[2].isEmpty() ? null : parts[2];
            long expireMillis = Long.parseLong(parts[3]);
            LocalDateTime expireAt = TimeUtil.millisToLocalDateTime(expireMillis);
            if (expireAt.isBefore(TimeUtil.currentLocalDateTime())) {
                log.debug("file token expired by time, token={}", token);
                return null;
            }
            return new FileToken(token, uid, op, fileId, expireAt);
        } catch (NumberFormatException e) {
            log.warn("file token parse error, token={}, value={}", token, encoded, e);
            return null;
        }
    }

    /**
     * 生成令牌缓存键。
     *
     * @param token 令牌字符串。
     * @return 缓存键。
     */
    private String buildCacheKey(String token) {
        return "file:token:" + token;
    }

    /**
     * 文件令牌信息。
     */
    @Data
    @AllArgsConstructor
    public static class FileToken {

        /**
         * 令牌字符串。
         */
        private String token;

        /**
         * 用户 ID。
         */
        private long uid;

        /**
         * 操作类型。
         */
        private String op;

        /**
         * 文件 ID。
         */
        private String fileId;

        /**
         * 过期时间。
         */
        private LocalDateTime expireAt;
    }
}
