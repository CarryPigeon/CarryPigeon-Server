package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPChannelMemberUpdaterNodeTests {

    @Test
    void process_shouldUpdateFieldsAndSave() throws Exception {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        when(dao.save(any())).thenReturn(true);

        CPChannelMemberUpdaterNode node = new CPChannelMemberUpdaterNode(dao);

        CPChannelMember member = new CPChannelMember()
                .setUid(1L)
                .setCid(2L)
                .setName("old")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, member);
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_NAME, "new");
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_AUTHORITY, CPChannelMemberAuthorityEnum.ADMIN.getAuthority());

        node.process(null, context);

        assertEquals("new", member.getName());
        assertEquals(CPChannelMemberAuthorityEnum.ADMIN, member.getAuthority());
        verify(dao).save(member);
    }

    @Test
    void process_saveFail_shouldThrowAndSetBusinessError() {
        ChannelMemberDao dao = mock(ChannelMemberDao.class);
        when(dao.save(any())).thenReturn(false);

        CPChannelMemberUpdaterNode node = new CPChannelMemberUpdaterNode(dao);

        CPFlowContext context = new CPFlowContext();
        context.set(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO, new CPChannelMember().setUid(1L).setCid(2L));

        CPProblemException ex = assertThrows(CPProblemException.class, () -> node.process(null, context));
        assertEquals(500, ex.getProblem().status());
        assertEquals("internal_error", ex.getProblem().reason());
    }
}
