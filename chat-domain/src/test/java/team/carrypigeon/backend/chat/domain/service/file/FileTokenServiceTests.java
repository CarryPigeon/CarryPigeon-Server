package team.carrypigeon.backend.chat.domain.service.file;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class FileTokenServiceTests {

    private static class InMemoryCache implements CPCache {
        private final Map<String, String> map = new ConcurrentHashMap<>();

        @Override
        public void set(String key, String value, int expireTime) {
            map.put(key, value);
        }

        @Override
        public String get(String key) {
            return map.get(key);
        }

        @Override
        public String getAndDelete(String key) {
            return map.remove(key);
        }

        @Override
        public String getAndSet(String key, String value, int expireTime) {
            return map.put(key, value);
        }

        @Override
        public boolean exists(String key) {
            return map.containsKey(key);
        }

        @Override
        public boolean delete(String key) {
            return map.remove(key) != null;
        }
    }

    @Test
    void createToken_thenConsume_shouldReturnTokenAndBeOneTime() {
        InMemoryCache cache = new InMemoryCache();
        FileTokenService service = new FileTokenService(cache);

        String token = service.createToken(1L, "UPLOAD", null, 60);
        assertNotNull(token);
        assertFalse(token.isBlank());

        FileTokenService.FileToken fileToken = service.consume(token);
        assertNotNull(fileToken);
        assertEquals(1L, fileToken.getUid());
        assertEquals("UPLOAD", fileToken.getOp());
        assertNull(fileToken.getFileId());

        assertNull(service.consume(token));
    }

    @Test
    void consume_invalidOrExpiredOrMalformed_shouldReturnNull() {
        InMemoryCache cache = new InMemoryCache();
        FileTokenService service = new FileTokenService(cache);

        assertNull(service.consume(null));
        assertNull(service.consume(""));

        cache.set("file:token:t1", "bad", 60);
        assertNull(service.consume("t1"));

        cache.set("file:token:t2", "1:UPLOAD::not-number", 60);
        assertNull(service.consume("t2"));

        long expiredMillis = TimeUtil.localDateTimeToMillis(LocalDateTime.now().minusSeconds(10));
        cache.set("file:token:t3", "1:DOWNLOAD:fid:" + expiredMillis, 60);
        assertNull(service.consume("t3"));
    }
}
