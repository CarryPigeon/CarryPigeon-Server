package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowConnectionInfo;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailRateLimitCheckerTests {

    @Test
    void process_missingEmail_shouldThrowAndSetArgsError() {
        EmailRateLimitChecker checker = new EmailRateLimitChecker(new InMemoryCPCache());

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_shortWindowLimited_shouldThrowAndSetTooManyRequests() {
        InMemoryCPCache cache = new InMemoryCPCache();
        EmailRateLimitChecker checker = new EmailRateLimitChecker(cache);

        String email = "a@b.com";
        cache.set("email_rate:ip_email:unknown:" + email, "5", 60);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setConnectionInfo(new CPFlowConnectionInfo().setRemoteIp(null).setRemoteAddress("x"));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(429, ex.getProblem().status());
        assertEquals("rate_limited", ex.getProblem().reason().code());
    }

    @Test
    void process_dailyWindowLimited_shouldThrowAndSetTooManyRequests() {
        InMemoryCPCache cache = new InMemoryCPCache();
        EmailRateLimitChecker checker = new EmailRateLimitChecker(cache);

        String email = "a@b.com";
        cache.set("email_rate:ip_email:unknown:" + email, "1", 60);
        cache.set("email_rate_day:ip_email:unknown:" + email, "30", 24 * 60 * 60);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setConnectionInfo(new CPFlowConnectionInfo().setRemoteIp(null).setRemoteAddress("x"));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(429, ex.getProblem().status());
        assertEquals("rate_limited", ex.getProblem().reason().code());
    }

    @Test
    void process_nonNumericCacheValue_shouldFallbackTo1() throws Exception {
        InMemoryCPCache cache = new InMemoryCPCache();
        EmailRateLimitChecker checker = new EmailRateLimitChecker(cache);

        String email = "a@b.com";
        cache.set("email_rate:ip_email:unknown:" + email, "oops", 60);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setConnectionInfo(new CPFlowConnectionInfo().setRemoteIp(null).setRemoteAddress("x"));

        checker.process(null, context);

        assertEquals("1", cache.get("email_rate:ip_email:unknown:" + email));
        assertEquals("1", cache.get("email_rate_day:ip_email:unknown:" + email));
    }

    private static final class InMemoryCPCache implements CPCache {
        private final Map<String, String> data = new HashMap<>();

        /**
         * 写入测试数据。
         *
         * @param key 测试输入参数
         * @param value 测试输入参数
         * @param expireTime 测试输入参数
         */
        @Override
        public void set(String key, String value, int expireTime) {
            data.put(key, value);
        }

        /**
         * 返回测试数据。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public String get(String key) {
            return data.get(key);
        }

        /**
         * 读取并删除测试数据。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public String getAndDelete(String key) {
            return data.remove(key);
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
            String old = data.get(key);
            data.put(key, value);
            return old;
        }

        /**
         * 判断测试数据是否存在。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public boolean exists(String key) {
            return data.containsKey(key);
        }

        /**
         * 删除测试数据。
         *
         * @param key 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public boolean delete(String key) {
            return data.remove(key) != null;
        }
    }
}
