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

        /**
         * 写入测试数据。
         *
         * @param key 测试输入参数
         * @param value 测试输入参数
         * @param expireTime 测试输入参数
         */
        @Override
        public void set(String key, String value, int expireTime) {
            map.put(key, value);
        }

        /**
         * 返回测试数据。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public String get(String key) {
            return map.get(key);
        }

        /**
         * 读取并删除测试数据。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public String getAndDelete(String key) {
            return map.remove(key);
        }

        /**
         * 读取旧值并写入新值。
         *
         * @param key 测试输入参数
         * @param value 测试输入参数
         * @param expireTime 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public String getAndSet(String key, String value, int expireTime) {
            return map.put(key, value);
        }

        /**
         * 判断测试数据是否存在。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public boolean exists(String key) {
            return map.containsKey(key);
        }

        /**
         * 删除测试数据。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
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
