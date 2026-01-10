package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.chat.domain.cmp.info.CheckResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelAdminCheckerTests {

    @Test
    void process_hardMode_notInChannel_shouldThrow() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelAdminChecker checker = new CPChannelAdminChecker(memberDao) {
            @Override
            protected boolean isSoftMode() {
                return false;
            }
        };

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeUserKeys.USER_INFO_ID, 2L);

        when(memberDao.getMember(2L, 1L)).thenReturn(null);

        assertThrows(CPReturnException.class, () -> checker.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("you are not in this channel", response.getData().get("msg").asText());
    }

    @Test
    void process_hardMode_notAdmin_shouldThrow() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelAdminChecker checker = new CPChannelAdminChecker(memberDao) {
            @Override
            protected boolean isSoftMode() {
                return false;
            }
        };

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER));

        assertThrows(CPReturnException.class, () -> checker.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("you are not the admin of this channel", response.getData().get("msg").asText());
    }

    @Test
    void process_softMode_shouldWriteCheckResult() throws Exception {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelAdminChecker checker = new CPChannelAdminChecker(memberDao) {
            @Override
            protected boolean isSoftMode() {
                return true;
            }
        };

        CPFlowContext notInChannel = new CPFlowContext();
        notInChannel.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        notInChannel.setData(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(null);
        checker.process(null, notInChannel);
        CheckResult result1 = notInChannel.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result1);
        assertFalse(result1.state());
        assertEquals("not in channel", result1.msg());
        assertNull(notInChannel.getData(CPNodeCommonKeys.RESPONSE));

        CPFlowContext notAdmin = new CPFlowContext();
        notAdmin.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        notAdmin.setData(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER));
        checker.process(null, notAdmin);
        CheckResult result2 = notAdmin.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result2);
        assertFalse(result2.state());
        assertEquals("not admin", result2.msg());

        CPFlowContext ok = new CPFlowContext();
        ok.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        ok.setData(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.ADMIN));
        checker.process(null, ok);
        CheckResult result3 = ok.getData(CPNodeCommonKeys.CHECK_RESULT);
        assertNotNull(result3);
        assertTrue(result3.state());
        assertNull(result3.msg());
    }
}
