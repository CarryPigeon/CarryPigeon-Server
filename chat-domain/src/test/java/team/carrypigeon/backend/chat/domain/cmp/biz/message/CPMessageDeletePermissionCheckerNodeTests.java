package team.carrypigeon.backend.chat.domain.cmp.biz.message;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
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
        missingMessage.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        assertThrows(CPReturnException.class, () -> node.process(null, missingMessage));
        assertArgsError(missingMessage);

        CPFlowContext missingOperator = new CPFlowContext();
        missingOperator.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage());
        assertThrows(CPReturnException.class, () -> node.process(null, missingOperator));
        assertArgsError(missingOperator);
    }

    @Test
    void process_messageSendTimeNull_shouldThrowArgsError() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage().setId(2L).setSendTime(null));

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertArgsError(context);
    }

    @Test
    void process_whenTimeout_shouldThrowBusinessError() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, 1L);
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(1L)
                .setCid(3L)
                .setSendTime(TimeUtil.getCurrentLocalTime().minusSeconds(121)));

        assertThrows(CPReturnException.class, () -> node.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("message delete timeout", response.getData().get("msg").asText());
    }

    @Test
    void process_ownerDelete_shouldReturnWithoutResponse() throws Exception {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        long uid = 1L;
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, uid);
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(uid)
                .setCid(3L)
                .setSendTime(LocalDateTime.now().minusSeconds(10)));

        node.process(null, context);
        assertNull(context.getData(CPNodeCommonKeys.RESPONSE));
        verify(memberDao, never()).getMember(anyLong(), anyLong());
    }

    @Test
    void process_notOwnerAndNotAdmin_shouldThrowNoPermission() {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        long operatorUid = 9L;
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, operatorUid);
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(1L)
                .setCid(3L)
                .setSendTime(LocalDateTime.now().minusSeconds(10)));

        when(memberDao.getMember(operatorUid, 3L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.MEMBER));

        assertThrows(CPReturnException.class, () -> node.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("no permission to delete message", response.getData().get("msg").asText());
    }

    @Test
    void process_notOwnerButAdmin_shouldReturnWithoutResponse() throws Exception {
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPMessageDeletePermissionCheckerNode node = new CPMessageDeletePermissionCheckerNode(memberDao);

        long operatorUid = 9L;
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeCommonKeys.SESSION_ID, operatorUid);
        context.setData(CPNodeMessageKeys.MESSAGE_INFO, new CPMessage()
                .setId(2L)
                .setUid(1L)
                .setCid(3L)
                .setSendTime(LocalDateTime.now().minusSeconds(10)));

        when(memberDao.getMember(operatorUid, 3L)).thenReturn(new CPChannelMember().setAuthority(CPChannelMemberAuthorityEnum.ADMIN));

        node.process(null, context);
        assertNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    private static void assertArgsError(CPFlowContext context) {
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("error args", response.getData().get("msg").asText());
    }
}

