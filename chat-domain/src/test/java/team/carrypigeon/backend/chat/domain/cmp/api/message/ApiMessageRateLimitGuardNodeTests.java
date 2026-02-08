package team.carrypigeon.backend.chat.domain.cmp.api.message;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.support.dao.InMemoryDaoConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiMessageRateLimitGuardNodeTests {

    @Test
    void process_disabled_shouldPass() {
        CpApiProperties props = new CpApiProperties();
        props.getApi().getMessageRateLimit().setEnabled(false);

        ApiMessageRateLimitGuardNode node = new ApiMessageRateLimitGuardNode(new InMemoryDaoConfig.InMemoryCPCache(), props);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 2L);
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");

        assertDoesNotThrow(() -> node.process(null, context));
    }

    @Test
    void process_coreText_exceedLimit_shouldThrowRateLimited() {
        CpApiProperties props = new CpApiProperties();
        props.getApi().getMessageRateLimit().setEnabled(true);
        props.getApi().getMessageRateLimit().setCoreText(new CpApiProperties.RateLimitWindow(60, 1));

        ApiMessageRateLimitGuardNode node = new ApiMessageRateLimitGuardNode(new InMemoryDaoConfig.InMemoryCPCache(), props);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 2L);
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Core:Text");

        assertDoesNotThrow(() -> node.process(null, context));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(429, ex.getProblem().status());
        assertEquals("rate_limited", ex.getProblem().reason());
        assertNotNull(ex.getProblem().details());
        assertTrue(ex.getProblem().details() instanceof Map);
        //noinspection unchecked
        Map<String, Object> details = (Map<String, Object>) ex.getProblem().details();
        assertTrue(details.containsKey("retry_after_ms"));
        Object v = details.get("retry_after_ms");
        assertNotNull(v);
        assertTrue(v instanceof Number);
        assertTrue(((Number) v).longValue() > 0);
    }

    @Test
    void process_pluginDomain_exceedLimit_shouldThrowRateLimited() {
        CpApiProperties props = new CpApiProperties();
        props.getApi().getMessageRateLimit().setEnabled(true);
        props.getApi().getMessageRateLimit().setPlugin(new CpApiProperties.RateLimitWindow(60, 1));

        ApiMessageRateLimitGuardNode node = new ApiMessageRateLimitGuardNode(new InMemoryDaoConfig.InMemoryCPCache(), props);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 10L);
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 20L);
        context.set(CPNodeMessageKeys.MESSAGE_INFO_DOMAIN, "Math:Formula");

        assertDoesNotThrow(() -> node.process(null, context));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(429, ex.getProblem().status());
        assertEquals("rate_limited", ex.getProblem().reason());
    }
}

