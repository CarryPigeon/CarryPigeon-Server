package team.carrypigeon.backend.chat.domain.controller;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.application.create.CPChannelCreateApplicationVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list.CPChannelListApplicationVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.application.process.CPChannelProcessApplicationVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.time.LocalDateTime;

/**
 * 集成测试：频道申请相关 Controller 对应的 LiteFlow 链路。
 *
 * 覆盖：
 * - /core/channel/application/create
 * - /core/channel/application/process
 * - /core/channel/application/list
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ChannelApplicationControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ChannelDao channelDao;

    @Autowired
    private ChannelApplicationDAO channelApplicationDAO;

    @Autowired
    private ChannelMemberDao channelMemberDao;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @After
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testChannelApplicationCreate_success() {
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

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelCreateApplicationVO vo = new CPChannelCreateApplicationVO(cid, "join please");
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/application/create", null, context);
        Assert.assertTrue(resp.isSuccess());

        // 应该有一条 pending 申请
        CPChannelApplication[] apps = channelApplicationDAO.getByCid(cid, 0, 10);
        Assert.assertEquals(1, apps.length);
        Assert.assertEquals(uid, apps[0].getUid().longValue());
        Assert.assertEquals(CPChannelApplicationStateEnum.PENDING, apps[0].getState());
    }

    @Test
    public void testChannelApplicationCreate_channelFixed_shouldFail() {
        long uid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("fixed")
                .setOwner(-1L) // 固定频道
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelCreateApplicationVO vo = new CPChannelCreateApplicationVO(cid, "join");
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/application/create", null, context);
        Assert.assertFalse(resp.isSuccess());
    }

    @Test
    public void testChannelApplicationProcess_approved_success() {
        long ownerUid = 9000L;
        long applicantUid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(ownerUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        // 预置一条 pending 申请
        CPChannelApplication app = new CPChannelApplication()
                .setId(1L)
                .setUid(applicantUid)
                .setCid(cid)
                .setState(CPChannelApplicationStateEnum.PENDING)
                .setMsg("join")
                .setApplyTime(LocalDateTime.now());
        channelApplicationDAO.save(app);

        // 预置 owner 成员（管理员）
        CPChannelMember ownerMember = new CPChannelMember()
                .setId(10L)
                .setUid(ownerUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(ownerMember);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, ownerUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        // result=1 -> approved
        CPChannelProcessApplicationVO vo = new CPChannelProcessApplicationVO(app.getId(), 1);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/application/process", null, context);
        Assert.assertTrue(resp.isSuccess());

        // 申请状态应为 APPROVED
        CPChannelApplication afterApp = channelApplicationDAO.getById(app.getId());
        Assert.assertEquals(CPChannelApplicationStateEnum.APPROVED, afterApp.getState());

        // 申请人应成为频道成员
        CPChannelMember newMember = channelMemberDao.getMember(applicantUid, cid);
        Assert.assertNotNull(newMember);
    }

    @Test
    public void testChannelApplicationProcess_rejected_success() {
        long ownerUid = 9000L;
        long applicantUid = 1000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(ownerUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelApplication app = new CPChannelApplication()
                .setId(2L)
                .setUid(applicantUid)
                .setCid(cid)
                .setState(CPChannelApplicationStateEnum.PENDING)
                .setMsg("join")
                .setApplyTime(LocalDateTime.now());
        channelApplicationDAO.save(app);

        CPChannelMember ownerMember = new CPChannelMember()
                .setId(10L)
                .setUid(ownerUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(ownerMember);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, ownerUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        // result=2 -> rejected
        CPChannelProcessApplicationVO vo = new CPChannelProcessApplicationVO(app.getId(), 2);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/application/process", null, context);
        Assert.assertTrue(resp.isSuccess());

        CPChannelApplication afterApp = channelApplicationDAO.getById(app.getId());
        Assert.assertEquals(CPChannelApplicationStateEnum.REJECTED, afterApp.getState());

        // 申请人不应被加入成员
        CPChannelMember member = channelMemberDao.getMember(applicantUid, cid);
        Assert.assertNull(member);
    }

    @Test
    public void testChannelApplicationList_success() {
        long ownerUid = 9000L;
        long cid = 2000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(ownerUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember ownerMember = new CPChannelMember()
                .setId(10L)
                .setUid(ownerUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(ownerMember);

        // 预置一条申请
        CPChannelApplication app = new CPChannelApplication()
                .setId(3L)
                .setUid(1000L)
                .setCid(cid)
                .setState(CPChannelApplicationStateEnum.PENDING)
                .setMsg("join")
                .setApplyTime(LocalDateTime.now());
        channelApplicationDAO.save(app);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, ownerUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelListApplicationVO vo = new CPChannelListApplicationVO(cid, 0, 10);
        Assert.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/application/list", null, context);
        Assert.assertTrue(resp.isSuccess());
    }
}
