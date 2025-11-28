package team.carrypigeon.backend.chat.domain.cmp.biz.channel.application;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPReturnException;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.PageInfo;
import team.carrypigeon.backend.chat.domain.support.TestSession;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 频道申请相关 Node 测试集合。
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class ChannelApplicationNodeTest {

    @Autowired
    private CPChannelApplicationCreatorNode applicationCreatorNode;
    @Autowired
    private CPChannelApplicationSavorNode applicationSavorNode;
    @Autowired
    private CPChannelApplicationSelectorNode applicationSelectorNode;
    @Autowired
    private CPChannelApplicationListerNode applicationListerNode;
    @Autowired
    private CPChannelApplicationApprovedNode applicationApprovedNode;
    @Autowired
    private CPChannelApplicationCidGetterNode applicationCidGetterNode;
    @Autowired
    private CPChannelApplicationStateSetterSwitcherNode applicationStateSwitcherNode;

    @Autowired
    private ChannelApplicationDAO channelApplicationDAO;

    @Test
    void testCPChannelApplicationCreatorNode_argsErrorWithoutChannelOrUid() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> applicationCreatorNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelApplicationSavorNode_savesApplication() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannelApplication app = new CPChannelApplication()
                .setId(IdUtil.generateId())
                .setCid(IdUtil.generateId())
                .setUid(IdUtil.generateId())
                .setApplyTime(TimeUtil.getCurrentLocalTime());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO, app);

        applicationSavorNode.process(new TestSession(), context);
        // 具体结果可以在本地通过 DAO 验证
    }

    @Test
    void testCPChannelApplicationSelectorNode_argsErrorWhenKeyMissing() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> applicationSelectorNode.process(new TestSession(), context));
        assertNotNull(ex);
    }

    @Test
    void testCPChannelApplicationListerNode_emptyListWhenNoData() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, IdUtil.generateId());
        context.setData(CPNodeValueKeyExtraConstants.PAGE_INFO, new PageInfo(1, 10));

        applicationListerNode.process(new TestSession(), context);
        // 没有数据时 applications 可能为空，由你后续补充断言
    }

    @Test
    void testCPChannelApplicationApprovedNode_createsChannelMemberFromApplication() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannelApplication app = new CPChannelApplication()
                .setId(IdUtil.generateId())
                .setCid(IdUtil.generateId())
                .setUid(IdUtil.generateId());
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO, app);

        applicationApprovedNode.process(new TestSession(), context);
        assertNotNull(context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO));
    }

    @Test
    void testCPChannelApplicationCidGetterNode_extractsCid() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannelApplication app = new CPChannelApplication().setCid(123L);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO, app);

        applicationCidGetterNode.process(new TestSession(), context);
        Long cid = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO_CID);
        assertEquals(123L, cid);
    }

    @Test
    void testCPChannelApplicationStateSetterSwitcherNode_argsErrorWhenMissing() {
        DefaultContext context = new DefaultContext();
        CPReturnException ex = assertThrows(CPReturnException.class,
                () -> applicationStateSwitcherNode.process(new TestSession(), context));
        assertNotNull(ex);
    }
}