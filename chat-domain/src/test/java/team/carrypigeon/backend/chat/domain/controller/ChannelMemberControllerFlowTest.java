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
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.member.get.CPChannelGetMemberVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.member.delete.CPChannelDeleteMemberVO;
import team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list.CPChannelListMemberVO;
import team.carrypigeon.backend.chat.domain.support.ChatDomainTestConfig;
import team.carrypigeon.backend.chat.domain.support.InMemoryDatabase;
import team.carrypigeon.backend.chat.domain.support.TestSession;

import java.time.LocalDateTime;

/**
 * 集成测试：频道成员相关 Controller 对应的 LiteFlow 链路。
 *
 * 覆盖：
 * - /core/channel/member/list
 * - /core/channel/member/delete
 * - /core/channel/member/get
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ChatDomainTestConfig.class)
public class ChannelMemberControllerFlowTest {

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
    public void testChannelMemberList_success() {
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

        CPChannelListMemberVO vo = new CPChannelListMemberVO(cid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/member/list", null, context);
        Assertions.assertTrue(resp.isSuccess());
    }

    @Test
    public void testChannelMemberGet_success() {
        long selfUid = 1000L;
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

        CPChannelMember selfMember = new CPChannelMember()
                .setId(1L)
                .setUid(selfUid)
                .setCid(cid)
                .setName("")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(selfMember);

        CPChannelMember targetMember = new CPChannelMember()
                .setId(2L)
                .setUid(targetUid)
                .setCid(cid)
                .setName("target")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(targetMember);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, selfUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelGetMemberVO vo = new CPChannelGetMemberVO(cid, targetUid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/member/get", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelMember result = context.getData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(targetUid, result.getUid());
        Assertions.assertEquals(cid, result.getCid());
    }

    @Test
    public void testChannelMemberDelete_success() {
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

        CPChannelDeleteMemberVO vo = new CPChannelDeleteMemberVO(cid, targetUid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/member/delete", null, context);
        Assertions.assertTrue(resp.isSuccess());

        CPChannelMember after = channelMemberDao.getMember(targetUid, cid);
        Assertions.assertNull(after);
    }

    @Test
    public void testChannelMemberGet_notInChannel_shouldFail() {
        long selfUid = 1000L;
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

        // 仅保存目标成员，当前会话用户不在频道中
        CPChannelMember targetMember = new CPChannelMember()
                .setId(2L)
                .setUid(targetUid)
                .setCid(cid)
                .setName("target")
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER)
                .setJoinTime(LocalDateTime.now());
        channelMemberDao.save(targetMember);

        TestSession session = new TestSession();
        session.setAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, selfUid);

        CPFlowContext context = new CPFlowContext();
        context.setData("session", session);

        CPChannelGetMemberVO vo = new CPChannelGetMemberVO(cid, targetUid);
        Assertions.assertTrue(vo.insertData(context));

        LiteflowResponse resp = flowExecutor.execute2Resp("/core/channel/member/get", null, context);
        Assertions.assertFalse(resp.isSuccess());
    }
}
