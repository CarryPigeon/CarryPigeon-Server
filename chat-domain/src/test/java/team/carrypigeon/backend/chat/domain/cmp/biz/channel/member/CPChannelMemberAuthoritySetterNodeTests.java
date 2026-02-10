package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelMemberAuthoritySetterNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        TestableNode node = new TestableNode("admin");
        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_setAdmin_shouldSetAuthority() throws Exception {
        TestableNode node = new TestableNode("admin");
        CPChannelMember member = new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.set(CPFlowKeys.SESSION_UID, 1L);

        node.process(null, context);
        assertEquals(CPChannelMemberAuthorityEnum.ADMIN, member.getAuthority());
    }

    @Test
    void process_setMember_notOwner_shouldThrowAuthorityError() {
        TestableNode node = new TestableNode("member");
        CPChannelMember member = new CPChannelMember();
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.set(CPFlowKeys.SESSION_UID, 2L);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("not_channel_owner", ex.getProblem().reason().code());
    }

    @Test
    void process_setMember_owner_shouldSetAuthority() throws Exception {
        TestableNode node = new TestableNode("member");
        CPChannelMember member = new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.set(CPFlowKeys.SESSION_UID, 1L);

        node.process(null, context);
        assertEquals(CPChannelMemberAuthorityEnum.MEMBER, member.getAuthority());
    }

    @Test
    void process_invalidAuthority_shouldThrowArgsError() {
        TestableNode node = new TestableNode("bad");
        CPChannelMember member = new CPChannelMember();
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.set(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.set(CPFlowKeys.SESSION_UID, 1L);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    private static final class TestableNode extends CPChannelMemberAuthoritySetterNode {
        private final String mode;

        /**
         * 构造测试辅助对象。
         *
         * @param mode 测试输入参数
         */
        private TestableNode(String mode) {
            this.mode = mode;
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
            if ("key".equals(key) && clazz == String.class) {
                return clazz.cast(mode);
            }
            return null;
        }
    }
}
