package team.carrypigeon.backend.chat.domain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create.CPMessageCreateVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.delete.CPMessageDeleteVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list.CPMessageListVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.get.CPMessageGetVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.update.CPMessageReadStateUpdateVO;
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
 * - /core/channel/message/get
 * - /core/channel/message/read/state/update
 * - /core/channel/message/read/state/get
 */
@ExtendWith(SpringExtension.class)
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
    private ChannelReadStateDao channelReadStateDao;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
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

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        // 构造最简单的文本消息
        ObjectNode data = objectMapper.createObjectNode();
        data.put("text", "hello");
        CPMessageCreateVO vo = new CPMessageCreateVO("Core:Text",cid, data);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/create", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPMessage[] stored = channelMessageDao.getBefore(cid, LocalDateTime.now(), 10);
        Assertions.assertTrue(stored.length >= 1);
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

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPMessageDeleteVO vo = new CPMessageDeleteVO(message.getId());
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/delete", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPMessage deleted = channelMessageDao.getById(message.getId());
        Assertions.assertNull(deleted);
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

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        long nowMillis = System.currentTimeMillis();
        CPMessageListVO vo = new CPMessageListVO(cid, nowMillis, 10);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/list", null, context);
        Assertions.assertTrue(resp.isSuccess());
    }

    @Test
    public void testChannelMessageGet_success() {
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
                .setSendTime(LocalDateTime.now());
        channelMessageDao.save(message);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPMessageGetVO vo = new CPMessageGetVO(message.getId());
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/get", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPMessage result = context.getData(CPNodeMessageKeys.MESSAGE_INFO);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(message.getId(), result.getId());
    }

    @Test
    public void testChannelMessageGet_notInChannel_shouldFail() {
        long uid = 1000L;
        long otherUid = 2000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(otherUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        // 仅将消息所属用户加入频道，当前会话用户不在频道中
        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(otherUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        CPMessage message = new CPMessage()
                .setId(10L)
                .setUid(otherUid)
                .setCid(cid)
                .setDomain("Core:Text")
                .setData(objectMapper.createObjectNode().put("text", "hello"))
                .setSendTime(LocalDateTime.now());
        channelMessageDao.save(message);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPMessageGetVO vo = new CPMessageGetVO(message.getId());
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/get", null, context);
        Assertions.assertFalse(resp.isSuccess());
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

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        long startMillis = System.currentTimeMillis() - 10 * 60 * 1000L;
        CPMessageGetUnreadVO vo = new CPMessageGetUnreadVO(cid, startMillis);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/unread/get", null, context);
        Assertions.assertTrue(resp.isSuccess());

        Long unread = context.getData(CPNodeMessageKeys.MESSAGE_UNREAD_COUNT);
        Assertions.assertNotNull(unread);
        Assertions.assertTrue(unread >= 1);
    }

    @Test
    public void testChannelMessageReadStateUpdate_success() {
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

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        long lastReadTimeMillis = System.currentTimeMillis();
        CPMessageReadStateUpdateVO vo = new CPMessageReadStateUpdateVO(cid, lastReadTimeMillis);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/read/state/update", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelReadState state = channelReadStateDao.getByUidAndCid(uid, cid);
        Assertions.assertNotNull(state);
        Assertions.assertEquals(uid, state.getUid());
        Assertions.assertEquals(cid, state.getCid());
        Assertions.assertTrue(state.getLastReadTime() > 0);
    }

    @Test
    public void testChannelMessageReadStateGet_success() {
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

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        // 初次获取，若没有记录，应返回 lastReadTime = 0
        team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.get.CPMessageReadStateGetVO vo =
                new team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.get.CPMessageReadStateGetVO(cid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/message/read/state/get", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelReadState state = context.getData(
                team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        Assertions.assertNotNull(state);
        Assertions.assertEquals(uid, state.getUid());
        Assertions.assertEquals(cid, state.getCid());
        Assertions.assertEquals(0L, state.getLastReadTime());
    }
}
