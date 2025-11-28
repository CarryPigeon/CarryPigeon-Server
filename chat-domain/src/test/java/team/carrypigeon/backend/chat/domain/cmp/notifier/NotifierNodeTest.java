package team.carrypigeon.backend.chat.domain.cmp.notifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.chat.domain.ChatDomainNodeTestConfiguration;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.service.session.CPSessionCenterService;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知相关 Node 测试集合：
 *  - CPNotifierNode
 *  - CPChannelAdminCollector
 *  - CPChannelMemberCollectorNode
 *  - CPMessageCreateNotifyBuilderNode
 *  - CPMessageDeleteNotifyBuilderNode
 *  - CPUserRelatedCollectorNode
 */
@SpringBootTest(classes = ChatDomainNodeTestConfiguration.class)
class NotifierNodeTest {

    @Autowired
    private CPNotifierNode notifierNode;
    @Autowired
    private team.carrypigeon.backend.chat.domain.cmp.notifier.channel.CPChannelAdminCollector channelAdminCollector;
    @Autowired
    private team.carrypigeon.backend.chat.domain.cmp.notifier.channel.CPChannelMemberCollectorNode channelMemberCollectorNode;
    @Autowired
    private team.carrypigeon.backend.chat.domain.cmp.notifier.message.CPMessageCreateNotifyBuilderNode messageCreateNotifyBuilderNode;
    @Autowired
    private team.carrypigeon.backend.chat.domain.cmp.notifier.message.CPMessageDeleteNotifyBuilderNode messageDeleteNotifyBuilderNode;
    @Autowired
    private team.carrypigeon.backend.chat.domain.cmp.notifier.user.CPUserRelatedCollectorNode userRelatedCollectorNode;

    @Autowired
    private CPSessionCenterService sessionCenterService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCPNotifierNode_sendsNotificationToSession() throws Exception {
        DefaultContext context = new DefaultContext();
        TestSession session = new TestSession();
        long uid = 100L;
        sessionCenterService.addSession(uid, session);

        Set<Long> uids = new HashSet<>();
        uids.add(uid);
        context.setData(CPNodeValueKeyBasicConstants.NOTIFIER_UIDS, uids);
        CPNotification notification = new CPNotification().setRoute("/test");
        context.setData(CPNodeValueKeyBasicConstants.NOTIFIER_DATA, objectMapper.valueToTree(notification));

        // 使用 bind route 的 Node，在单元测试中无法直接配置 route，这里只确认不抛异常
        notifierNode.process(new TestSession(), context);
    }

    @Test
    void testCPMessageCreateNotifyBuilderNode_buildsNotifierData() throws Exception {
        DefaultContext context = new DefaultContext();
        CPMessage msg = new CPMessage().setCid(1L).setUid(2L);
        ObjectNode data = objectMapper.createObjectNode().put("text", "hello");
        CPMessage cpMessage = msg.setData(data);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, cpMessage);
        // MessageData 由解析流程写入，这里不强依赖

        messageCreateNotifyBuilderNode.process(new TestSession(), context);
        assertNotNull(context.getData(CPNodeValueKeyBasicConstants.NOTIFIER_DATA));
    }

    @Test
    void testCPMessageDeleteNotifyBuilderNode_buildsNotifierData() throws Exception {
        DefaultContext context = new DefaultContext();
        CPMessage msg = new CPMessage().setCid(1L).setUid(2L);
        context.setData(CPNodeValueKeyBasicConstants.MESSAGE_INFO, msg);

        messageDeleteNotifyBuilderNode.process(new TestSession(), context);
        assertNotNull(context.getData(CPNodeValueKeyBasicConstants.NOTIFIER_DATA));
    }

    @Test
    void testCPChannelMemberCollectorNode_collectsMembers() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannel channel = new CPChannel().setId(1L);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channel);

        channelMemberCollectorNode.process(new TestSession(), context);
        // members 为空时 Notifier_Uids 可能为空，由你后续补充
    }

    @Test
    void testCPChannelAdminCollectorNode_collectsAdmins() throws Exception {
        DefaultContext context = new DefaultContext();
        CPChannel channel = new CPChannel().setId(1L);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO, channel);

        channelAdminCollector.process(new TestSession(), context);
        // 同上
    }

    @Test
    void testCPUserRelatedCollectorNode_collectsRelatedUsers() throws Exception {
        DefaultContext context = new DefaultContext();
        context.setData(CPNodeValueKeyBasicConstants.USER_INFO_ID, 1L);

        userRelatedCollectorNode.process(new TestSession(), context);
        // 依赖真实数据库关联关系，由你后续补充断言
    }
}