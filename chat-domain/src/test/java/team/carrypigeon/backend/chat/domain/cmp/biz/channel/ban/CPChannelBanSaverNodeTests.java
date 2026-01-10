package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelBanSaverNodeTests {

    @Test
    void process_argsMissing_shouldThrowArgsError() {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanSaverNode node = new CPChannelBanSaverNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_saveFail_shouldThrowBusinessError() {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(null);
        when(dao.save(any())).thenReturn(false);
        CPChannelBanSaverNode node = new CPChannelBanSaverNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID, 2L);
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_DURATION, 60);
        context.setData(CPNodeUserKeys.USER_INFO_ID, 9L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("error saving channel ban", response.getData().get("msg").asText());
    }

    @Test
    void process_newBan_shouldCreateAndSave() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(null);
        when(dao.save(any())).thenReturn(true);
        CPChannelBanSaverNode node = new CPChannelBanSaverNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID, 2L);
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_DURATION, 60);
        context.setData(CPNodeUserKeys.USER_INFO_ID, 9L);

        node.process(null, context);

        CPChannelBan ban = context.getData(CPNodeChannelBanKeys.CHANNEL_BAN_INFO);
        assertNotNull(ban);
        assertEquals(1L, ban.getCid());
        assertEquals(2L, ban.getUid());
        assertEquals(9L, ban.getAid());
        assertEquals(60, ban.getDuration());
        assertNotNull(ban.getCreateTime());
        assertTrue(ban.getId() > 0);
    }

    @Test
    void process_existingBan_shouldUpdateAndSave() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBan existing = new CPChannelBan().setId(7L).setCid(1L).setUid(2L).setAid(0L).setDuration(1);
        when(dao.getByChannelIdAndUserId(2L, 1L)).thenReturn(existing);
        when(dao.save(any())).thenReturn(true);
        CPChannelBanSaverNode node = new CPChannelBanSaverNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, 1L);
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID, 2L);
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_DURATION, 60);
        context.setData(CPNodeUserKeys.USER_INFO_ID, 9L);

        node.process(null, context);

        CPChannelBan ban = context.getData(CPNodeChannelBanKeys.CHANNEL_BAN_INFO);
        assertSame(existing, ban);
        assertEquals(7L, ban.getId());
        assertEquals(9L, ban.getAid());
        assertEquals(60, ban.getDuration());
        assertNotNull(ban.getCreateTime());
        verify(dao).save(existing);
    }
}

