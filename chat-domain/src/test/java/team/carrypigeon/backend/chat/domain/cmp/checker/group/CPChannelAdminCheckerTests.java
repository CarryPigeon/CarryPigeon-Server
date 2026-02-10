package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelAdminCheckerTests {

    @Test
    void process_hardMode_notInChannel_shouldThrow() {
        ChannelDao channelDao = mock(ChannelDao.class);
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelAdminChecker checker = new CPChannelAdminChecker(channelDao, memberDao) {
            /**
             * 测试辅助方法。
             *
             * @return 测试辅助方法返回结果
             */
            @Override
            protected boolean isSoftMode() {
                return false;
            }
        };

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeUserKeys.USER_INFO_ID, 2L);

        when(memberDao.getMember(2L, 1L)).thenReturn(null);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("not_channel_member", ex.getProblem().reason().code());
    }

    @Test
    void process_hardMode_notAdmin_shouldThrow() {
        ChannelDao channelDao = mock(ChannelDao.class);
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelAdminChecker checker = new CPChannelAdminChecker(channelDao, memberDao) {
            /**
             * 测试辅助方法。
             *
             * @return 测试辅助方法返回结果
             */
            @Override
            protected boolean isSoftMode() {
                return false;
            }
        };

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("not_channel_admin", ex.getProblem().reason().code());
    }

    @Test
    void process_softMode_shouldWriteCheckResult() throws Exception {
        ChannelDao channelDao = mock(ChannelDao.class);
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelAdminChecker checker = new CPChannelAdminChecker(channelDao, memberDao) {
            /**
             * 测试辅助方法。
             *
             * @return 测试辅助方法返回结果
             */
            @Override
            protected boolean isSoftMode() {
                return true;
            }
        };

        CPFlowContext notInChannel = new CPFlowContext();
        notInChannel.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        notInChannel.set(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(null);
        checker.process(null, notInChannel);
        CheckResult result1 = notInChannel.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result1);
        assertFalse(result1.state());
        assertEquals("not in channel", result1.msg());
        assertNull(notInChannel.get(CPFlowKeys.RESPONSE));

        CPFlowContext notAdmin = new CPFlowContext();
        notAdmin.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        notAdmin.set(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER));
        checker.process(null, notAdmin);
        CheckResult result2 = notAdmin.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result2);
        assertFalse(result2.state());
        assertEquals("not admin", result2.msg());

        CPFlowContext ok = new CPFlowContext();
        ok.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        ok.set(CPNodeUserKeys.USER_INFO_ID, 2L);
        when(memberDao.getMember(2L, 1L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.ADMIN));
        checker.process(null, ok);
        CheckResult result3 = ok.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result3);
        assertTrue(result3.state());
        assertNull(result3.msg());
    }
}
