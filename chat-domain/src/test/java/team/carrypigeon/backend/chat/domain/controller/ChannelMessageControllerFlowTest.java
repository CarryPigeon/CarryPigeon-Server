package team.carrypigeon.backend.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create.CPMessageCreateVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.delete.CPMessageDeleteVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list.CPMessageListVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get.CPMessageGetUnreadVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.time.LocalDateTime;

/**
 * 集成测试：频道消息相关 Controller 对应的 LiteFlow 链路。
 *
 * 覆盖：
 * - /core/channel/message/create
 * - /core/channel/message/delete
 * - /core/channel/message/list
 * - /core/channel/message/unread/get
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ChannelMessageControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ChannelDao channelDao;

    @Autowired
    private ChannelMemberDao channelMemberDao;

    @Autowired
    private ChannelMessageDao channelMessageDao;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @Autowired
    private ObjectMapper objectMapper;

    @After
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testChannelMessageCreate_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(9999L)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(uid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        // 构造最简单的文本消息
        ObjectNode data = objectMapper.createObjectNode();
        data.put("text", "hello");
        CPMessageCreateVO vo = new CPMessageCreateVO("Core:Text",cid, data);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/create", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPMessage[] stored = channelMessageDao.getBefore(cid, LocalDateTime.now(), 10);
        Assert.assertTrue(stored.length >= 1);
    }

    @Test
    public void testChannelMessageDelete_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(uid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(uid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        // 预置一条消息
        CPMessage message = new CPMessage()
                .setId(10L)
                .setUid(uid)
                .setCid(cid)
                .setDomain("Core:Text")
                .setData(objectMapper.createObjectNode().put("text", "hello"))
                .setSendTime(LocalDateTime.now());
        channelMessageDao.save(message);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPMessageDeleteVO vo = new CPMessageDeleteVO(message.getId());
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/delete", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPMessage deleted = channelMessageDao.getById(message.getId());
        Assert.assertNull(deleted);
    }

    @Test
    public void testChannelMessageList_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(uid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(uid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        CPMessage message = new CPMessage()
                .setId(10L)
                .setUid(uid)
                .setCid(cid)
                .setDomain("Core:Text")
                .setData(objectMapper.createObjectNode().put("text", "hello"))
                .setSendTime(LocalDateTime.now().minusMinutes(1));
        channelMessageDao.save(message);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        long nowMillis = System.currentTimeMillis();
        CPMessageListVO vo = new CPMessageListVO(cid, nowMillis, 10);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/list", null, context);
        Assert.assertTrue(resp.isSuccess());
    }

    @Test
    public void testChannelMessageUnreadGet_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(uid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(uid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        LocalDateTime start = LocalDateTime.now().minusMinutes(10);

        CPMessage message = new CPMessage()
                .setId(10L)
                .setUid(uid)
                .setCid(cid)
                .setDomain("Core:Text")
                .setData(objectMapper.createObjectNode().put("text", "hello"))
                .setSendTime(LocalDateTime.now());
        channelMessageDao.save(message);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        long startMillis = System.currentTimeMillis() - 10 * 60 * 1000L;
        CPMessageGetUnreadVO vo = new CPMessageGetUnreadVO(cid, startMillis);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/unread/get", null, context);
        Assert.assertTrue(resp.isSuccess());

        Long unread = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_UNREAD_COUNT);
        Assert.assertNotNull(unread);
        Assert.assertTrue(unread >= 1);
    }
}

