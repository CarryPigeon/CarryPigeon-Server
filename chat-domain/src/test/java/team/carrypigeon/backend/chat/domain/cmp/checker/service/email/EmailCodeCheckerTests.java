package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailCodeCheckerTests {

    @Test
    void process_paramMissing_shouldThrowServerError() {
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(new InMemoryCPCache(), null);

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_codeMismatch_hard_shouldThrowBusinessError() {
        InMemoryCPCache cache = new InMemoryCPCache();
        cache.set("a@b.com:code", "123", 300);
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(cache, null);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");
        context.set(CPNodeValueKeyExtraConstants.EMAIL_CODE, 456);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("email_code_invalid", ex.getProblem().reason().code());
    }

    @Test
    void process_codeMismatch_soft_shouldWriteCheckResult() throws Exception {
        InMemoryCPCache cache = new InMemoryCPCache();
        cache.set("a@b.com:code", "123", 300);
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(cache, "soft");

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");
        context.set(CPNodeValueKeyExtraConstants.EMAIL_CODE, 456);

        checker.process(null, context);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("email code error", result.msg());
    }

    @Test
    void process_codeMatch_soft_shouldWriteCheckResultSuccess() throws Exception {
        InMemoryCPCache cache = new InMemoryCPCache();
        cache.set("a@b.com:code", "123", 300);
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(cache, "soft");

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");
        context.set(CPNodeValueKeyExtraConstants.EMAIL_CODE, 123);

        checker.process(null, context);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertNull(result.msg());
    }

    private static final class TestableEmailCodeChecker extends EmailCodeChecker {
        private final String type;

        /**
         * 构造测试辅助对象。
         *
         * @param cache 测试输入参数
         * @param type 测试输入参数
         */
        private TestableEmailCodeChecker(CPCache cache, String type) {
            super(cache);
            this.type = type;
        }

        /**
         * 测试辅助方法。
         *
         * @param key 测试输入参数
         * @param clazz 测试输入参数
         * @return 测试辅助方法返回结果
         */
        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("type".equals(key) && clazz == String.class) {
                return clazz.cast(type);
            }
            return null;
        }
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
