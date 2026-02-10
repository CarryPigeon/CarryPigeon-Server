package team.carrypigeon.backend.chat.domain.cmp.checker.group;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.api.chat.domain.flow.CheckResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelBanCheckerNodeTests {

    @Test
    void process_softMode_noBan_shouldMarkSuccess() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanCheckerNode checker = new CPChannelBanCheckerNode(dao) {
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

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(null);

        checker.process(null, context);

        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    @Test
    void process_hardMode_validBan_shouldThrow() {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanCheckerNode checker = new CPChannelBanCheckerNode(dao) {
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

        CPChannelBan ban = new CPChannelBan()
                .setId(1L)
                .setCid(1L)
                .setUid(2L)
                .setDuration(60)
                .setCreateTime(LocalDateTime.now());

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(ban);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> checker.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("user_muted", ex.getProblem().reason().code());
    }

    @Test
    void process_softMode_validBan_shouldMarkFail() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanCheckerNode checker = new CPChannelBanCheckerNode(dao) {
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

        CPChannelBan ban = new CPChannelBan()
                .setId(1L)
                .setCid(1L)
                .setUid(2L)
                .setDuration(60)
                .setCreateTime(LocalDateTime.now());

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(ban);

        checker.process(null, context);

        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertFalse(result.state());
        assertEquals("banned", result.msg());
        assertNull(context.get(CPFlowKeys.RESPONSE));
    }

    @Test
    void process_softMode_expiredBan_shouldDeleteAndMarkSuccess() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanCheckerNode checker = new CPChannelBanCheckerNode(dao) {
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

        CPChannelBan ban = new CPChannelBan()
                .setId(1L)
                .setCid(1L)
                .setUid(2L)
                .setDuration(1)
                .setCreateTime(LocalDateTime.now().minusSeconds(10));

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(ban);
        when(dao.delete(ban)).thenReturn(true);

        checker.process(null, context);
        verify(dao, times(1)).delete(ban);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }

    @Test
    void process_softMode_expiredBan_whenDeleteFails_shouldStillMarkSuccess() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanCheckerNode checker = new CPChannelBanCheckerNode(dao) {
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

        CPChannelBan ban = new CPChannelBan()
                .setId(1L)
                .setCid(1L)
                .setUid(2L)
                .setDuration(1)
                .setCreateTime(LocalDateTime.now().minusSeconds(10));

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(ban);
        when(dao.delete(ban)).thenReturn(false);

        checker.process(null, context);
        verify(dao, times(1)).delete(ban);
        CheckResult result = context.get(CPFlowKeys.CHECK_RESULT);
        assertNotNull(result);
        assertTrue(result.state());
    }
}
