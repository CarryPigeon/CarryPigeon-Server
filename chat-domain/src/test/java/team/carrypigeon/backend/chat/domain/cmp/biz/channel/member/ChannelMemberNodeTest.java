package team.carrypigeon.backend.chat.domain.cmp.biz.channel.member;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 频道成员相关 Node 测试集合。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class ChannelMemberNodeTest {

    @Autowired
    private CPChannelMemberCreatorNode memberCreatorNode;
    @Autowired
    private CPChannelMemberSaverNode memberSaverNode;
    @Autowired
    private CPChannelMemberSelectorNode memberSelectorNode;
    @Autowired
    private CPChannelMemberUpdaterNode memberUpdaterNode;
    @Autowired
    private CPChannelMemberListerNode memberListerNode;
    @Autowired
    private CPChannelMemberDeleterNode memberDeleterNode;
    @Autowired
    private CPChannelMemberAuthoritySetterNode memberAuthoritySetterNode;

    @Autowired
    private ChannelMemberDao channelMemberDao;

    @Test
    void testCPChannelMemberCreatorNode_createsMemberFromChannelAndUser() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannel channel = new CPChannel().setId(IdUtil.generateId());
        long uid = IdUtil.generateId();
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channel);
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_ID, uid);

        memberCreatorNode.process(new TestSession(), context);
        CPChannelMember member = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO);
        assertNotNull(member);
        assertEquals(channel.getId(), member.getCid());
        assertEquals(uid, member.getUid());
    }

    @Test
    void testCPChannelMemberSaverNode_savesMember() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannelMember member = new CPChannelMember()
                .setId(IdUtil.generateId())
                .setCid(IdUtil.generateId())
                .setUid(IdUtil.generateId())
                .setJoinTime(TimeUtil.getCurrentLocalTime());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO, member);

        memberSaverNode.process(new TestSession(), context);
        // 这里只验证流程，具体 DB 状态由你后续补充
    }

    @Test
    void testCPChannelMemberSelectorNode_argsErrorWhenKeyMissing() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> memberSelectorNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelMemberUpdaterNode_updatesFields() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannelMember member = new CPChannelMember()
                .setId(IdUtil.generateId())
                .setCid(IdUtil.generateId())
                .setUid(IdUtil.generateId());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO, member);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_NAME, "newName");

        memberUpdaterNode.process(new TestSession(), context);
        CPChannelMember updated = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO);
        assertEquals("newName", updated.getName());
    }

    @Test
    void testCPChannelMemberListerNode_noMembersForChannel() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_CID, IdUtil.generateId());

        memberListerNode.process(new TestSession(), context);
        // 当没有成员时，集合可能为 null 或空，留给你调整
    }

    @Test
    void testCPChannelMemberDeleterNode_deleteNonExistWritesError() {
        DefaultContext context = new DefaultContext();
        CPChannelMember member = new CPChannelMember().setId(IdUtil.generateId());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO, member);

        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> memberDeleterNode.process(new TestSession(), context));
        assertNotNull(ex);
        CPResponse resp = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNotNull(resp);
    }

    @Test
    void testCPChannelMemberAuthoritySetterNode_argsErrorWhenMissingData() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> memberAuthoritySetterNode.process(new TestSession(), context));
        assertNotNull(ex);
    }
}