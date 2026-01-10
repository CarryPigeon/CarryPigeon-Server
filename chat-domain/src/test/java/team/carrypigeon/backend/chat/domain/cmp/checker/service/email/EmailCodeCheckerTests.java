package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailCodeCheckerTests {

    @Test
    void process_paramMissing_shouldThrowServerError() {
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(new InMemoryCPCache(), null);

        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> checker.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(500, response.getCode());
        assertEquals("email code param error", response.getData().get("msg").asText());
    }

    @Test
    void process_codeMismatch_hard_shouldThrowBusinessError() {
        InMemoryCPCache cache = new InMemoryCPCache();
        cache.set("a@b.com:code", "123", 300);
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(cache, null);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");
        context.setData(CPNodeValueKeyExtraConstants.EMAIL_CODE, 456);

        assertThrows(CPReturnException.class, () -> checker.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("email code error", response.getData().get("msg").asText());
    }

    @Test
    void process_codeMismatch_soft_shouldWriteCheckResult() throws Exception {
        InMemoryCPCache cache = new InMemoryCPCache();
        cache.set("a@b.com:code", "123", 300);
        TestableEmailCodeChecker checker = new TestableEmailCodeChecker(cache, "soft");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");
        context.setData(CPNodeValueKeyExtraConstants.EMAIL_CODE, 456);

        checker.process(null, context);
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
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
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, "a@b.com");
        context.setData(CPNodeValueKeyExtraConstants.EMAIL_CODE, 123);

        checker.process(null, context);
        CheckResult result = context.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertNull(result.msg());
    }

    private static final class TestableEmailCodeChecker extends EmailCodeChecker {
        private final String type;

        private TestableEmailCodeChecker(CPCache cache, String type) {
            super(cache);
            this.type = type;
        }

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

        @Override
        public void set(String key, String value, int expireTime) {
            data.put(key, value);
        }

        @Override
        public String get(String key) {
            return data.get(key);
        }

        @Override
        public String getAndDelete(String key) {
            return data.remove(key);
        }

        @Override
        public String getAndSet(String key, String value, int expireTime) {
            String old = data.get(key);
            data.put(key, value);
            return old;
        }

        @Override
        public boolean exists(String key) {
            return data.containsKey(key);
        }

        @Override
        public boolean delete(String key) {
            return data.remove(key) != null;
        }
    }
}

