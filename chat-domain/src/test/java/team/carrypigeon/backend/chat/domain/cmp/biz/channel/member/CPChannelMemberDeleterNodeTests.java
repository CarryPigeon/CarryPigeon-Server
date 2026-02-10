package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CPChannelMemberDeleterNodeTests {

    @Test
    void process_adminMember_shouldRefuseDelete() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        ChannelDao channelDao = mock(ChannelDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        CPChannelMemberDeleterNode node = new CPChannelMemberDeleterNode(dao, channelDao, ws);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("forbidden", ex.getProblem().reason().code());
        verify(dao, never()).delete(any());
    }

    @Test
    void process_deleteFail_shouldReturnBusinessError() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        ChannelDao channelDao = mock(ChannelDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.delete(any())).thenReturn(false);
        CPChannelMemberDeleterNode node = new CPChannelMemberDeleterNode(dao, channelDao, ws);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason().code());
        verify(dao).delete(member);
    }

    @Test
    void process_deleteSuccess_shouldNotThrow() throws Exception {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        ChannelDao channelDao = mock(ChannelDao.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.delete(any())).thenReturn(true);
        CPChannelMemberDeleterNode node = new CPChannelMemberDeleterNode(dao, channelDao, ws);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);

        node.process(null, context);
        verify(dao).delete(member);
    }
}
