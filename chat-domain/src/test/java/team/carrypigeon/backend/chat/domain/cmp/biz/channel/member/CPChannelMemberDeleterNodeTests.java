package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelMemberDeleterNodeTests {

    @Test
    void process_adminMember_shouldRefuseDelete() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        CPChannelMemberDeleterNode node = new CPChannelMemberDeleterNode(dao);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        assertThrows(CPReturnException.class, () -> node.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("cannot delete channel admin", response.getData().get("msg").asText());
        verify(dao, never()).delete(any());
    }

    @Test
    void process_deleteFail_shouldReturnBusinessError() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        when(dao.delete(any())).thenReturn(false);
        CPChannelMemberDeleterNode node = new CPChannelMemberDeleterNode(dao);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        assertThrows(CPReturnException.class, () -> node.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("error deleting channel member", response.getData().get("msg").asText());
        verify(dao).delete(member);
    }

    @Test
    void process_deleteSuccess_shouldNotThrow() throws Exception {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        when(dao.delete(any())).thenReturn(true);
        CPChannelMemberDeleterNode node = new CPChannelMemberDeleterNode(dao);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        node.process(null, context);
        assertNull(context.getData(CPNodeCommonKeys.RESPONSE));
        verify(dao).delete(member);
    }
}
