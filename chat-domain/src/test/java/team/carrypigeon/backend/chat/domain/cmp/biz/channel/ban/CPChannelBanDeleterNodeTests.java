package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelBanDeleterNodeTests {

    @Test
    void process_deleteSuccess_shouldNotThrow() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        when(dao.delete(any())).thenReturn(true);
        CPChannelBanDeleterNode node = new CPChannelBanDeleterNode(dao);

        CPChannelBan ban = new CPChannelBan().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_INFO, ban);

        node.process(null, context);
        assertNull(context.getData(CPNodeCommonKeys.RESPONSE));
        verify(dao).delete(ban);
    }

    @Test
    void process_deleteFail_shouldThrowBusinessError() {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        when(dao.delete(any())).thenReturn(false);
        CPChannelBanDeleterNode node = new CPChannelBanDeleterNode(dao);

        CPChannelBan ban = new CPChannelBan().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_INFO, ban);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("error deleting channel ban", response.getData().get("msg").asText());
    }
}

