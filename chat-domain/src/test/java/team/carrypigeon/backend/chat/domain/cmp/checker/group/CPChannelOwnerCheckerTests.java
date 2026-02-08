package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelOwnerCheckerTests {

    @Test
    void process_notOwner_hard_shouldThrowBusinessError() {
        TestableCPChannelOwnerChecker node = new TestableCPChannelOwnerChecker(null);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setId(1L).setOwner(2L));
        context.set(CPNodeUserKeys.USER_INFO_ID, 3L);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("not_channel_owner", ex.getProblem().reason());
    }

    @Test
    void process_notOwner_soft_shouldWriteCheckResult() throws Exception {
        TestableCPChannelOwnerChecker node = new TestableCPChannelOwnerChecker("soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setId(1L).setOwner(2L));
        context.set(CPNodeUserKeys.USER_INFO_ID, 3L);

        node.process(null, context);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("not owner", result.msg());
    }

    @Test
    void process_owner_soft_shouldWriteSuccess() throws Exception {
        TestableCPChannelOwnerChecker node = new TestableCPChannelOwnerChecker("soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setId(1L).setOwner(3L));
        context.set(CPNodeUserKeys.USER_INFO_ID, 3L);

        node.process(null, context);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    private static final class TestableCPChannelOwnerChecker extends CPChannelOwnerChecker {
        private final String type;

        private TestableCPChannelOwnerChecker(String type) {
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
}
