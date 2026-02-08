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
 * Simple in-memory file token service.
 * Tokens are one-time and have a short TTL, used to authorize
 * HTTP upload/download requests that are initiated via Netty.
 */
@Slf4j
@Service
@AllArgsConstructor
public class FileTokenService {

    private final CPCache cpCache;

    /**
     * Create a one-time token.
     *
     * @param uid    owner user id
     * @param op     operation, e.g. "UPLOAD" or "DOWNLOAD"
     * @param fileId related file identifier (may be null for upload)
     * @param ttlSec time-to-live in seconds
     * @return generated token string
     */
    public String createToken(long uid, String op, String fileId, long ttlSec) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = TimeUtil.currentLocalDateTime().plusSeconds(ttlSec);
        FileToken fileToken = new FileToken(token, uid, op, fileId, expireAt);
        // encode as uid:op:fileId:expireAtMillis
        long expireMillis = TimeUtil.localDateTimeToMillis(expireAt);
        String encoded = uid + ":" + op + ":" + (fileId == null ? "" : fileId) + ":" + expireMillis;
        cpCache.set(buildCacheKey(token), encoded, (int) ttlSec);
        log.debug("create file token, token={}, uid={}, op={}, fileId={} ttlSec={}",
                token, uid, op, fileId, ttlSec);
        return token;
    }

    /**
     * Get and remove a token. Returns null if token does not exist or expired.
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

    private String buildCacheKey(String token) {
        return "file:token:" + token;
    }

    @Data
    @AllArgsConstructor
    public static class FileToken {
        private String token;
        private long uid;
        private String op;
        private String fileId;
        private LocalDateTime expireAt;
    }
}
