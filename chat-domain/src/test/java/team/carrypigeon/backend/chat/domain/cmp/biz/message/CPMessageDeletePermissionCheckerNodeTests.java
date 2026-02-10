package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPMessageDeletePermissionCheckerNodeTests {

    @Test
    void process_missingMessageOrOperator_shouldThrowArgsError() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        CPFlowContext missingMessage = new CPFlowContext();
        missingMessage.set(CPFlowKeys.SESSION_UID, 1L);
        CPProblemException ex1 = assertThrows(CPProblemException.class, () -> node.process(null, missingMessage));
        assertEquals(422, ex1.getProblem().status());
        assertEquals("validation_failed", ex1.getProblem().reason().code());

        CPFlowContext missingOperator = new CPFlowContext();
        missingOperator.set(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage());
        CPProblemException ex2 = assertThrows(CPProblemException.class, () -> node.process(null, missingOperator));
        assertEquals(422, ex2.getProblem().status());
        assertEquals("validation_failed", ex2.getProblem().reason().code());
    }

    @Test
    void process_messageSendTimeNull_shouldThrowArgsError() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage().setId(2L).setSendTime(null));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_whenTimeout_shouldThrowBusinessError() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, 1L);
        context.set(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(1L)
                .setCid(3L)
                .setSendTime(TimeUtil.currentLocalDateTime().minusSeconds(121)));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(409, ex.getProblem().status());
        assertEquals("conflict", ex.getProblem().reason().code());
    }

    @Test
    void process_ownerDelete_shouldReturnWithoutResponse() throws Exception {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        long uid = 1L;
        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, uid);
        context.set(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(uid)
                .setCid(3L)
                .setSendTime(LocalDateTime.now().minusSeconds(10)));

        node.process(null, context);
        verify(memberDao, never()).getMember(anyLong(), anyLong());
    }

    @Test
    void process_notOwnerAndNotAdmin_shouldThrowNoPermission() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        long operatorUid = 9L;
        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, operatorUid);
        context.set(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(1L)
                .setCid(3L)
                .setSendTime(LocalDateTime.now().minusSeconds(10)));

        when(memberDao.getMember(operatorUid, 3L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(403, ex.getProblem().status());
        assertEquals("forbidden", ex.getProblem().reason().code());
    }

    @Test
    void process_notOwnerButAdmin_shouldReturnWithoutResponse() throws Exception {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        long operatorUid = 9L;
        CPFlowContext context = new CPFlowContext();
        context.set(CPFlowKeys.SESSION_UID, operatorUid);
        context.set(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(1L)
                .setCid(3L)
                .setSendTime(LocalDateTime.now().minusSeconds(10)));

        when(memberDao.getMember(operatorUid, 3L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.ADMIN));

        node.process(null, context);
    }
}
