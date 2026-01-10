package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import static org.junit.jupiter.api.Assertions.*;

class CPChannelMemberAuthoritySetterNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        TestableNode node = new TestableNode("admin");
        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_setAdmin_shouldSetAuthority() throws Exception {
        TestableNode node = new TestableNode("admin");
        CPChannelMember member = new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.setData(CPNodeCommonKeys.SESSION_ID, 2L);

        node.process(null, context);
        assertEquals(CPChannelMemberAuthorityEnum.ADMIN, member.getAuthority());
    }

    @Test
    void process_setMember_notOwner_shouldThrowAuthorityError() {
        TestableNode node = new TestableNode("member");
        CPChannelMember member = new CPChannelMember();
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.setData(CPNodeCommonKeys.SESSION_ID, 2L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(300, response.getCode());
        assertEquals("you are the owner of this channel", response.getData().get("msg").asText());
    }

    @Test
    void process_setMember_owner_shouldSetAuthority() throws Exception {
        TestableNode node = new TestableNode("member");
        CPChannelMember member = new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);

        node.process(null, context);
        assertEquals(CPChannelMemberAuthorityEnum.MEMBER, member.getAuthority());
    }

    @Test
    void process_invalidAuthority_shouldThrowArgsError() {
        TestableNode node = new TestableNode("bad");
        CPChannelMember member = new CPChannelMember();
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO, new CPChannel().setOwner(1L));
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    private static final class TestableNode extends CPChannelMemberAuthoritySetterNode {
        private final String mode;

        private TestableNode(String mode) {
            this.mode = mode;
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("key".equals(key) && clazz == String.class) {
                return clazz.cast(mode);
            }
            return null;
        }
    }
}

