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
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list.CPChannelListBanResultItem;
import team.carrypigeon.backend.common.time.TimeUtil;

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
        assertThrows(CPReturnException.class, () -> node.process(null, context));

        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals(100, response.getCode());
        assertEquals("error args", response.getData().get("msg").asText());
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
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);

        node.process(null, context);

        @SuppressWarnings("unchecked")
        List<CPChannelListBanResultItem> items = (List<CPChannelListBanResultItem>) context.getData(CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(2L, items.get(0).getUid());
        assertEquals(3L, items.get(0).getAid());
        assertEquals(60, items.get(0).getDuration());
        assertEquals(TimeUtil.LocalDateTimeToMillis(now), items.get(0).getBanTime());

        verify(dao).delete(expired1);
        verify(dao).delete(expired2);
    }
}

