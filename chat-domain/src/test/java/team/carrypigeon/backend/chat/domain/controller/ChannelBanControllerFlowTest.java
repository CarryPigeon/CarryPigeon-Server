package team.carrypigeon.backend.chat.domain.controller;

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
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.create.CPChannelCreateBanVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.delete.CPChannelDeleteBanVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list.CPChannelListBanVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.time.LocalDateTime;

/**
 * 集成测试：频道封禁相关 Controller 对应的 LiteFlow 链路。
 *
 * 覆盖：
 * - /core/channel/ban/create
 * - /core/channel/ban/delete
 * - /core/channel/ban/list
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ChannelBanControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ChannelDao channelDao;

    @Autowired
    private ChannelMemberDao channelMemberDao;

    @Autowired
    private ChannelBanDAO channelBanDAO;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @AfterEach
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testChannelBanCreate_success() {
        long adminUid = 1000L;
        long targetUid = 2000L;
        long cid = 3000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(9999L)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        // 管理员成员
        CPChannelMember adminMember = new CPChannelMember()
                .setId(1L)
                .setUid(adminUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(adminMember);

        // 目标成员
        CPChannelMember targetMember = new CPChannelMember()
                .setId(2L)
                .setUid(targetUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(targetMember);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, adminUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelCreateBanVO vo = new CPChannelCreateBanVO(cid, targetUid, 60);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/ban/create", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelBan ban = channelBanDAO.getByChannelIdAndUserId(targetUid, cid);
        Assertions.assertNotNull(ban);
    }

    @Test
    public void testChannelBanCreate_targetIsAdmin_shouldFail() {
        long adminUid = 1000L;
        long cid = 3000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(9999L)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        // 管理员作为封禁目标
        CPChannelMember adminMember = new CPChannelMember()
                .setId(1L)
                .setUid(adminUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(adminMember);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, adminUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelCreateBanVO vo = new CPChannelCreateBanVO(cid, adminUid, 60);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/ban/create", null, context);
        Assertions.assertFalse(resp.isSuccess());

        CPChannelBan ban = channelBanDAO.getByChannelIdAndUserId(adminUid, cid);
        Assertions.assertNull(ban);
    }

    @Test
    public void testChannelBanDelete_success() {
        long adminUid = 1000L;
        long targetUid = 2000L;
        long cid = 3000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(9999L)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember adminMember = new CPChannelMember()
                .setId(1L)
                .setUid(adminUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(adminMember);

        CPChannelBan ban = new CPChannelBan()
                .setId(10L)
                .setCid(cid)
                .setUid(targetUid)
                .setAid(adminUid)
                .setDuration(60)
                .setCreateTime(LocalDateTime.now());
        channelBanDAO.save(ban);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, adminUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelDeleteBanVO vo = new CPChannelDeleteBanVO(ban.getCid(), ban.getUid());
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/ban/delete", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelBan deleted = channelBanDAO.getById(ban.getId());
        Assertions.assertNull(deleted);
    }

    @Test
    public void testChannelBanList_success() {
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

        CPChannelBan ban = new CPChannelBan()
                .setId(10L)
                .setCid(cid)
                .setUid(3000L)
                .setAid(uid)
                .setDuration(60)
                .setCreateTime(LocalDateTime.now());
        channelBanDAO.save(ban);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, uid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelListBanVO vo = new CPChannelListBanVO(cid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/ban/list", null, context);
        Assertions.assertTrue(resp.isSuccess());
    }
}
