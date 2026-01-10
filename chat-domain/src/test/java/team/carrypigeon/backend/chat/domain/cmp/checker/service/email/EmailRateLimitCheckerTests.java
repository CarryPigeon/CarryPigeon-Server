package team.carrypigeon.backend.chat.domain.cmp.checker.service.email;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowConnectionInfo;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.cache.CPCache;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailRateLimitCheckerTests {

    @Test
    void process_missingEmail_shouldThrowAndSetArgsError() {
        EmailRateLimitChecker checker = new EmailRateLimitChecker(new InMemoryCPCache());

        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> checker.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("error args", response.getData().get("msg").asText());
    }

    @Test
    void process_shortWindowLimited_shouldThrowAndSetTooManyRequests() {
        InMemoryCPCache cache = new InMemoryCPCache();
        EmailRateLimitChecker checker = new EmailRateLimitChecker(cache);

        String email = "a@b.com";
        cache.set("email_rate:ip_email:unknown:" + email, "5", 60);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setConnectionInfo(new CPFlowConnectionInfo().setRemoteIp(null).setRemoteAddress("x"));

        assertThrows(CPReturnException.class, () -> checker.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("too many email requests", response.getData().get("msg").asText());
    }

    @Test
    void process_dailyWindowLimited_shouldThrowAndSetTooManyRequests() {
        InMemoryCPCache cache = new InMemoryCPCache();
        EmailRateLimitChecker checker = new EmailRateLimitChecker(cache);

        String email = "a@b.com";
        cache.set("email_rate:ip_email:unknown:" + email, "1", 60);
        cache.set("email_rate_day:ip_email:unknown:" + email, "30", 24 * 60 * 60);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setConnectionInfo(new CPFlowConnectionInfo().setRemoteIp(null).setRemoteAddress("x"));

        assertThrows(CPReturnException.class, () -> checker.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("too many email requests", response.getData().get("msg").asText());
    }

    @Test
    void process_nonNumericCacheValue_shouldFallbackTo1() throws Exception {
        InMemoryCPCache cache = new InMemoryCPCache();
        EmailRateLimitChecker checker = new EmailRateLimitChecker(cache);

        String email = "a@b.com";
        cache.set("email_rate:ip_email:unknown:" + email, "oops", 60);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeValueKeyExtraConstants.EMAIL, email);
        context.setConnectionInfo(new CPFlowConnectionInfo().setRemoteIp(null).setRemoteAddress("x"));

        checker.process(null, context);

        assertEquals("1", cache.get("email_rate:ip_email:unknown:" + email));
        assertEquals("1", cache.get("email_rate_day:ip_email:unknown:" + email));
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

