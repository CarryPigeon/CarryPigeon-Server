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
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.create.CPChannelCreateAdminVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.delete.CPChannelDeleteAdminVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.time.LocalDateTime;

/**
 * 集成测试：频道管理员相关 Controller 对应的 LiteFlow 链路。
 * 覆盖：
 * - /core/channel/admin/create
 * - /core/channel/admin/delete
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ChannelAdminControllerFlowTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ChannelDao channelDao;

    @Autowired
    private ChannelMemberDao channelMemberDao;

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @AfterEach
    public void clearDatabase() {
        inMemoryDatabase.clearAll();
    }

    @Test
    public void testChannelCreateAdmin_success() {
        long ownerUid = 1000L;
        long memberUid = 2000L;
        long cid = 3000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(ownerUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(memberUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, ownerUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelCreateAdminVO vo = new CPChannelCreateAdminVO(cid, memberUid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/admin/create", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelMember after = channelMemberDao.getMember(memberUid, cid);
        Assertions.assertNotNull(after);
        Assertions.assertEquals(CPChannelMemberAuthorityEnum.ADMIN, after.getAuthority());
    }

    @Test
    public void testChannelCreateAdmin_notOwner() {
        long ownerUid = 1000L;
        long otherUid = 1001L;
        long memberUid = 2000L;
        long cid = 3000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(ownerUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(memberUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, otherUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelCreateAdminVO vo = new CPChannelCreateAdminVO(cid, memberUid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/admin/create", null, context);
        Assertions.assertFalse(resp.isSuccess());

        CPChannelMember after = channelMemberDao.getMember(memberUid, cid);
        Assertions.assertNotNull(after);
        Assertions.assertEquals(CPChannelMemberAuthorityEnum.MEMBER, after.getAuthority());
    }

    @Test
    public void testChannelDeleteAdmin_success() {
        long ownerUid = 1000L;
        long adminUid = 2000L;
        long cid = 3000L;

        CPChannel channel = new CPChannel()
                .setId(cid)
                .setName("test")
                .setOwner(ownerUid)
                .setBrief("")
                .setAvatar(-1L)
                .setCreateTime(LocalDateTime.now());
        channelDao.save(channel);

        CPChannelMember member = new CPChannelMember()
                .setId(1L)
                .setUid(adminUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(member);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, ownerUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelDeleteAdminVO vo = new CPChannelDeleteAdminVO(cid, adminUid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/admin/delete", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelMember after = channelMemberDao.getMember(adminUid, cid);
        Assertions.assertNotNull(after);
        Assertions.assertEquals(CPChannelMemberAuthorityEnum.MEMBER, after.getAuthority());
    }
}
