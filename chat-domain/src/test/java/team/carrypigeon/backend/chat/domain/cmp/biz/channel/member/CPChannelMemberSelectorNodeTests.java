package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelMemberSelectorNodeTests {

    @Test
    void process_modeId_shouldSelectAndCache() throws Exception {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        CPChannelMember entity = new CPChannelMember().setId(1L);
        when(dao.getById(1L)).thenReturn(entity);

        TestableCPChannelMemberSelectorNode node = new TestableCPChannelMemberSelectorNode(dao);
        node.setMode("id");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_ID, 1L);

        node.process(null, context);
        assertSame(entity, context.getData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO));

        node.process(null, context);
        verify(dao, times(1)).getById(1L);
    }

    @Test
    void process_modeCidWithUid_shouldSelect() throws Exception {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        CPChannelMember entity = new CPChannelMember().setId(1L).setUid(2L).setCid(3L);
        when(dao.getMember(2L, 3L)).thenReturn(entity);

        TestableCPChannelMemberSelectorNode node = new TestableCPChannelMemberSelectorNode(dao);
        node.setMode("CidWithUid");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, 2L);
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID, 3L);

        node.process(null, context);
        assertSame(entity, context.getData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO));
        verify(dao).getMember(2L, 3L);
    }

    @Test
    void process_invalidMode_shouldThrowArgsError() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        TestableCPChannelMemberSelectorNode node = new TestableCPChannelMemberSelectorNode(dao);
        node.setMode("unknown");

        CPFlowContext context = new CPFlowContext();
        assertThrows(CPReturnException.class, () -> node.process(null, context));
        assertNotNull(context.getData(CPNodeCommonKeys.RESPONSE));
    }

    @Test
    void process_notFound_shouldThrowBusinessError() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        when(dao.getById(1L)).thenReturn(null);

        TestableCPChannelMemberSelectorNode node = new TestableCPChannelMemberSelectorNode(dao);
        node.setMode("id");

        CPFlowContext context = new CPFlowContext();
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_ID, 1L);

        assertThrows(CPReturnException.class, () -> node.process(null, context));
        CPResponse response = context.getData(CPNodeCommonKeys.RESPONSE);
        assertNotNull(response);
        assertEquals("channel member not found", response.getData().get("msg").asText());
    }

    private static final class TestableCPChannelMemberSelectorNode extends CPChannelMemberSelectorNode {
        private String mode;

        private TestableCPChannelMemberSelectorNode(ChannelMemberDao channelMemberDao) {
            super(channelMemberDao);
        }

        private void setMode(String mode) {
            this.mode = mode;
        }

        @Override
        public <T> T getBindData(String key, Class<T> clazz) {
            if ("key".equals(key) && clazz == String.class) {
                return clazz.cast(mode);
            }
            return null;
        }
    }
}

