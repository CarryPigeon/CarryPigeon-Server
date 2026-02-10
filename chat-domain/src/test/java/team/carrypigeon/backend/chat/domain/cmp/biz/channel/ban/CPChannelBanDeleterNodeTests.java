package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.service.ws.ApiWsEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelBanDeleterNodeTests {

    @Test
    void process_deleteSuccess_shouldNotThrow() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.delete(any())).thenReturn(true);
        CPChannelBanDeleterNode node = new CPChannelBanDeleterNode(dao, ws);

        CPChannelBan ban = new CPChannelBan().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_INFO, ban);

        node.process(null, context);
        verify(dao).delete(ban);
    }

    @Test
    void process_deleteFail_shouldThrowBusinessError() {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        ApiWsEventPublisher ws = mock(ApiWsEventPublisher.class);
        when(dao.delete(any())).thenReturn(false);
        CPChannelBanDeleterNode node = new CPChannelBanDeleterNode(dao, ws);

        CPChannelBan ban = new CPChannelBan().setId(1L).setCid(2L).setUid(3L);
        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelBanKeys.CHANNEL_BAN_INFO, ban);

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason().code());
    }
}
