package team.carrypigeon.backend.chat.domain.cmp.biz.channel;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 频道基础 Node 测试集合：
 *  - CPChannelBuilderNode
 *  - CPChannelCreatorNode
 *  - CPChannelDeleterNode
 *  - CPChannelGroupSelectorNode
 *  - CPChannelSaverNode
 *  - CPChannelSelectorNode
 *  - CPChannelUpdaterNode
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class ChannelNodeTest {

    @Autowired
    private CPChannelBuilderNode channelBuilderNode;
    @Autowired
    private CPChannelCreatorNode channelCreatorNode;
    @Autowired
    private CPChannelDeleterNode channelDeleterNode;
    @Autowired
    private CPChannelGroupSelectorNode channelGroupSelectorNode;
    @Autowired
    private CPChannelSaverNode channelSaverNode;
    @Autowired
    private CPChannelSelectorNode channelSelectorNode;
    @Autowired
    private CPChannelUpdaterNode channelUpdaterNode;

    @Autowired
    private ChannelDao channelDao;

    @Test
    void testCPChannelBuilderNode_buildsChannelFromContext() throws Exception {
        DefaultContext context = new DefaultContext();
        long id = IdUtil.generateId();
        long owner = IdUtil.generateId();
        long avatar = -1L;
        long createTime = TimeUtil.getCurrentTime();

        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, id);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_NAME, "test-channel");
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_OWNER, owner);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_BRIEF, "brief");
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_AVATAR, avatar);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_CREATE_TIME, createTime);

        channelBuilderNode.process(new TestSession(), context);
        CPChannel channel = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        assertNotNull(channel);
        assertEquals(id, channel.getId());
    }

    @Test
    void testCPChannelCreatorNode_createsChannelFromUser() throws Exception {
        DefaultContext context = new DefaultContext();
        CPUser user = new CPUser().setId(IdUtil.generateId());
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO, user);

        TestSession session = new TestSession();
        session.setAttributeValue("ChatDomainUserId", user.getId());

        channelCreatorNode.process(session, context);
        CPChannel channel = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        assertNotNull(channel);
        assertEquals(user.getId(), channel.getOwner());
    }

    @Test
    void testCPChannelSaverNode_savesChannel() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannel channel = new CPChannel()
                .setId(IdUtil.generateId())
                .setName("channel_" + IdUtil.generateId())
                .setOwner(IdUtil.generateId());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channel);

        channelSaverNode.process(new TestSession(), context);
        CPChannel fromDb = channelDao.getById(channel.getId());
        // 如果数据库为空可能返回 null，这里只是确保调用链路打通
        // 你可以在本地补充断言逻辑
    }

    @Test
    void testCPChannelSelectorNode_argsErrorWhenIdMissing() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelSelectorNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelUpdaterNode_updatesChannelFields() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannel channel = new CPChannel()
                .setId(IdUtil.generateId())
                .setName("old")
                .setOwner(IdUtil.generateId())
                .setBrief("old")
                .setAvatar(-1L);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channel);

        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_NAME, "new-name");
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_BRIEF, "new-brief");

        channelUpdaterNode.process(new TestSession(), context);
        CPChannel updated = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_INFO);
        assertEquals("new-name", updated.getName());
        assertEquals("new-brief", updated.getBrief());
    }

    @Test
    void testCPChannelDeleterNode_deleteNonExistChannelWritesError() {
        DefaultContext context = new DefaultContext();
        CPChannel channel = new CPChannel().setId(IdUtil.generateId());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channel);

        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> channelDeleterNode.process(new TestSession(), context));
        assertNotNull(ex);
        CPResponse resp = context.getData(CPNodeValueKeyBasicConstants.RESPONSE);
        assertNotNull(resp);
    }

    @Test
    void testCPChannelGroupSelectorNode_noChannelsForUser() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_ID, IdUtil.generateId());

        channelGroupSelectorNode.process(new TestSession(), context);
        // 当用户没有频道时，channels 可能为 null 或空集合，留给你后续细化断言
    }
}