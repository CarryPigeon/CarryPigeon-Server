package team.carrypigeon.backend.chat.domain.cmp.biz.channel.ban;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelBanListerNodeTests {

    @Test
    void process_missingCid_shouldThrowArgsError() {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanListerNode node = new CPChannelBanListerNode(dao);

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }

    @Test
    void process_shouldConvertValidAndDeleteExpired() throws Exception {
        ChannelBanDAO dao = mock(ChannelBanDAO.class);
        CPChannelBanListerNode node = new CPChannelBanListerNode(dao);

        long cid = 10L;
        LocalDateTime now = LocalDateTime.now();
        CPChannelBan valid = new CPChannelBan()
                .setId(1L).setCid(cid).setUid(2L).setAid(3L).setDuration(60).setCreateTime(now);
        CPChannelBan expired1 = new CPChannelBan()
                .setId(2L).setCid(cid).setUid(4L).setAid(5L).setDuration(1).setCreateTime(now.minusSeconds(1000));
        CPChannelBan expired2 = new CPChannelBan()
                .setId(3L).setCid(cid).setUid(6L).setAid(7L).setDuration(1).setCreateTime(now.minusSeconds(1000));

        when(dao.getByChannelId(cid)).thenReturn(new CPChannelBan[]{valid, expired1, expired2});
        when(dao.delete(expired1)).thenReturn(true);
        when(dao.delete(expired2)).thenReturn(false);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);

        node.process(null, context);

        @SuppressWarnings("unchecked")
        List<CPChannelBan> items = (List<CPChannelBan>) context.get(CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertSame(valid, items.get(0));

        verify(dao).delete(expired1);
        verify(dao).delete(expired2);
    }
}
