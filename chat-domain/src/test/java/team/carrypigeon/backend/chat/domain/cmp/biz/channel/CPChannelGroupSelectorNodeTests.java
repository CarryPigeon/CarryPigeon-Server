package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelGroupSelectorNodeTests {

    @Test
    void process_shouldSelectFixedAndMemberChannels_andUseCache() throws Exception {
        ChannelDao channelDao = mock(ChannelDao.class);
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);

        CPChannel fixed = new CPChannel().setId(1L).setName("fixed");
        when(channelDao.getAllFixed()).thenReturn(new CPChannel[]{fixed});

        long uid = 99L;
        when(memberDao.getAllMemberByUserId(uid)).thenReturn(new CPChannelMember[]{
                new CPChannelMember().setCid(10L),
                new CPChannelMember().setCid(10L),
                new CPChannelMember().setCid(11L)
        });

        CPChannel c10 = new CPChannel().setId(10L).setName("c10");
        when(channelDao.getById(10L)).thenReturn(c10);
        when(channelDao.getById(11L)).thenReturn(null);

        CPChannelGroupSelectorNode node = new CPChannelGroupSelectorNode(channelDao, memberDao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);

        node.process(null, context);

        @SuppressWarnings("unchecked")
        Set<CPChannel> channels = (Set<CPChannel>) context.get(CPNodeChannelKeys.CHANNEL_INFO_LIST);
        assertNotNull(channels);
        assertEquals(2, channels.size());
        Set<Long> ids = channels.stream().map(CPChannel::getId).collect(Collectors.toSet());
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(10L));

        verify(channelDao, times(1)).getAllFixed();
        verify(memberDao, times(1)).getAllMemberByUserId(uid);
        verify(channelDao, times(1)).getById(10L);
        verify(channelDao, times(1)).getById(11L);
    }

    @Test
    void process_missingUserId_shouldThrowAndWriteArgsError() {
        ChannelDao channelDao = mock(ChannelDao.class);
        ChannelMemberDao memberDao = mock(ChannelMemberDao.class);
        CPChannelGroupSelectorNode node = new CPChannelGroupSelectorNode(channelDao, memberDao);

        CPFlowContext context = new CPFlowContext();
        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(422, ex.getProblem().status());
        assertEquals("validation_failed", ex.getProblem().reason().code());
    }
}
