package team.carrypigeon.backend.chat.domain.cmp.notifier.user;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeNotifierKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeUserKeys;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPUserRelatedCollectorNodeTests {

    @Test
    void process_shouldCreateSetAndCollectAllUids_withCacheForSameCid() throws Exception {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        long uid = 99L;
        CPChannelMember m1 = new CPChannelMember().setCid(10L);
        CPChannelMember m2 = new CPChannelMember().setCid(10L);
        when(dao.getAllMemberByUserId(uid)).thenReturn(new CPChannelMember[]{m1, m2});

        CPChannelMember c1 = new CPChannelMember().setUid(1L).setCid(10L);
        CPChannelMember c2 = new CPChannelMember().setUid(2L).setCid(10L);
        when(dao.getAllMember(10L)).thenReturn(new CPChannelMember[]{c1, c2});

        CPUserRelatedCollectorNode node = new CPUserRelatedCollectorNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeUserKeys.USER_INFO_ID, uid);
        Set<Long> pre = new HashSet<>();
        pre.add(0L);
        context.set(CPNodeNotifierKeys.NOTIFIER_UIDS, pre);

        node.process(null, context);

        @SuppressWarnings("unchecked")
        Set<Long> result = (Set<Long>) context.get(CPNodeNotifierKeys.NOTIFIER_UIDS);
        assertSame(pre, result);
        assertTrue(result.contains(0L));
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));

        verify(dao, times(1)).getAllMemberByUserId(uid);
        verify(dao, times(1)).getAllMember(10L);
    }
}
