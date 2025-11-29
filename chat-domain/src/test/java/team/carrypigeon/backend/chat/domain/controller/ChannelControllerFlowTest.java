package team.carrypigeon.backend.chat.domain.controller;

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
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.create.CPChannelCreateVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get.CPChannelGetProfileVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.data.update.CPChannelUpdateProfileVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.delete.CPChannelDeleteVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.list.CPChannelListVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.time.LocalDateTime;

/**
 * 集成测试：频道基础操作相关 Controller 对应的 LiteFlow 链路。
 *
 * 覆盖：
 * - /core/channel/create
 * - /core/channel/delete
 * - /core/channel/list
 * - /core/channel/profile/get
 * - /core/channel/profile/update
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ChannelControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ChannelDao channelDao;

    @Autowired
    private ChannelMemberDao channelMemberDao;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @After
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testChannelCreate_success() {
        long uid = 1000L;
        CPUser user = new CPUser()
                .setId(uid)
                .setUsername("u")
                .setAvatar(-1L)
                .setEmail("user@test.com")
                .setSex(null)
                .setBrief("")
                .setBirthday(LocalDateTime.now())
                .setRegisterTime(LocalDateTime.now());
        userDao.save(user);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPChannelCreateVO vo = new CPChannelCreateVO();
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/create", null, context);
        Assert.assertTrue(resp.isSuccess());

        // 用户应当加入一个频道，且该频道 owner 为 uid
        CPChannelMember[] members = channelMemberDao.getAllMemberByUserId(uid);
        Assert.assertEquals(1, members.length);
        long cid = members[0].getCid();
        CPChannel channel = channelDao.getById(cid);
        Assert.assertNotNull(channel);
        Assert.assertEquals(uid, channel.getOwner());
    }

    @Test
    public void testChannelDelete_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPUser user = new CPUser()
                .setId(uid)
                .setUsername("u")
                .setAvatar(-1L)
                .setEmail("user@test.com")
                .setSex(null)
                .setBrief("")
                .setBirthday(LocalDateTime.now())
                .setRegisterTime(LocalDateTime.now());
        userDao.save(user);

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("channel")
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

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPChannelDeleteVO vo = new CPChannelDeleteVO(cid);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/delete", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPChannel deleted = channelDao.getById(cid);
        Assert.assertNull(deleted);
    }

    @Test
    public void testChannelList_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPUser user = new CPUser()
                .setId(uid)
                .setUsername("u")
                .setAvatar(-1L)
                .setEmail("user@test.com")
                .setSex(null)
                .setBrief("")
                .setBirthday(LocalDateTime.now())
                .setRegisterTime(LocalDateTime.now());
        userDao.save(user);

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("channel")
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

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPChannelListVO vo = new CPChannelListVO();
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/list", null, context);
        Assert.assertTrue(resp.isSuccess());
    }

    @Test
    public void testChannelProfileGet_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("channel")
                .setOwner(uid)
                .setBrief("brief")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPChannelGetProfileVO vo = new CPChannelGetProfileVO(cid);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/profile/get", null, context);
        Assert.assertTrue(resp.isSuccess());
    }

    @Test
    public void testChannelProfileUpdate_success() {
        long uid = 1000L;
        long cid = 2000L;

        CPUser user = new CPUser()
                .setId(uid)
                .setUsername("u")
                .setAvatar(-1L)
                .setEmail("user@test.com")
                .setSex(null)
                .setBrief("")
                .setBirthday(LocalDateTime.now())
                .setRegisterTime(LocalDateTime.now());
        userDao.save(user);

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("channel")
                .setOwner(uid)
                .setBrief("brief")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        DefaultContext context = new DefaultContext();
        context.setData("session", session);

        CPChannelUpdateProfileVO vo = new CPChannelUpdateProfileVO(cid, "newName", uid, "newBrief", 2L);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/profile/update", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPChannel updated = channelDao.getById(cid);
        Assert.assertNotNull(updated);
        Assert.assertEquals("newName", updated.getName());
        Assert.assertEquals("newBrief", updated.getBrief());
        Assert.assertEquals(2L, updated.getAvatar());
    }
}

