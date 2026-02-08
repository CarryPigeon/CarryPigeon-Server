package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelBanTargetCheckerNodeTests {

    @Test
    void process_targetIsAdmin_soft_shouldWriteCheckResult() throws Exception {
        TestableCPChannelBanTargetCheckerNode node = new TestableCPChannelBanTargetCheckerNode("soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO,
                new CPChannelMember().setUid(1L).setCid(2L).setAuthority(CPChannelMemberAuthorityEnum.ADMIN));

        node.process(null, context);

        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("target is admin", result.msg());
    }

    @Test
    void process_targetIsAdmin_hard_shouldThrowBusinessError() {
        TestableCPChannelBanTargetCheckerNode node = new TestableCPChannelBanTargetCheckerNode(null);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO,
                new CPChannelMember().setUid(1L).setCid(2L).setAuthority(CPChannelMemberAuthorityEnum.ADMIN));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("cannot_ban_admin", ex.getProblem().reason());
    }

    @Test
    void process_targetNotAdmin_soft_shouldWriteSuccess() throws Exception {
        TestableCPChannelBanTargetCheckerNode node = new TestableCPChannelBanTargetCheckerNode("soft");
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO,
                new CPChannelMember().setUid(1L).setCid(2L).setAuthority(CPChannelMemberAuthorityEnum.MEMBER));

        node.process(null, context);

        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
        assertNull(result.msg());
    }

    private static final class TestableCPChannelBanTargetCheckerNode extends CPChannelBanTargetCheckerNode {
        private final String type;

        private TestableCPChannelBanTargetCheckerNode(String type) {
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
