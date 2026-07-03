package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.GetDefaultChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.GetSystemChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.query.ListChannelMembersQuery;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelUnread;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.TestContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.BASE_TIME;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.newContext;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.privateChannel;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.profile;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.systemChannel;

/**
 * 频道访问与读取契约测试。
 * 职责：验证频道获取、成员列表、已读状态与未读聚合的应用层读取语义。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
@Tag("contract")
class ChannelDomainApiAccessTests {

    /**
     * 验证默认频道存在时会返回稳定频道结果。
     */
    @Test
    @DisplayName("get default channel existing channel returns result")
    void getDefaultChannel_existingChannel_returnsResult() {
        TestContext context = newContext();
        ChannelAccessDomainApi service = context.createAccessService();

        var result = service.getDefaultChannel(new GetDefaultChannelCommand(1001L));

        assertEquals(1L, result.channelId());
        assertEquals(1L, result.conversationId());
        assertEquals("public", result.name());
        assertEquals("1001", result.ownerUid());
        assertEquals("public", result.type());
    }

    /**
     * 验证 system 频道存在且当前账户为成员时会返回稳定结果。
     */
    @Test
    @DisplayName("get system channel existing member returns result")
    void getSystemChannel_existingMember_returnsResult() {
        TestContext context = newContext();
        context.channelRepository.channels.put(2L, systemChannel());
        context.channelMemberRepository.save(new ChannelMember(2L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        ChannelAccessDomainApi service = context.createAccessService();

        var result = service.getSystemChannel(new GetSystemChannelCommand(1001L));

        assertEquals(2L, result.channelId());
        assertEquals("system", result.type());
    }

    /**
     * 验证 system 频道成员列表对普通成员不可见。
     */
    @Test
    @DisplayName("list channel members system channel throws forbidden problem")
    void listChannelMembers_systemChannel_throwsForbiddenProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(2L, systemChannel());
        context.channelMemberRepository.save(new ChannelMember(2L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        ChannelQueryDomainApi service = context.createQueryService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.listChannelMembers(new ListChannelMembersQuery(1001L, 2L))
        );

        assertEquals("system channel member list is not available", exception.getMessage());
    }

    /**
     * 验证已读状态更新会推进锚点并发布读状态变更。
     */
    @Test
    @DisplayName("update channel read state advances anchor and returns result")
    void updateChannelReadState_advancesAnchorAndReturnsResult() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
        context.messageRepository.save(new ChannelMessage(5001L, "550e8400-e29b-41d4-a716-446655440000", 9L, 9L, 1002L, "text", "hello", "hello", "hello", null, null, "sent", BASE_TIME));
        ChannelAccessDomainApi service = context.createAccessService();

        var result = service.updateChannelReadState(new UpdateChannelReadStateCommand(1001L, 9L, 5001L, BASE_TIME.plusSeconds(5).toEpochMilli()));

        assertEquals("9", result.cid());
        assertEquals("5001", result.lastReadMid());
        assertEquals(1, context.channelRealtimePublisher.readStateUpdates.size());
        assertEquals(5001L, context.channelRealtimePublisher.readStateUpdates.getFirst().lastReadMessageId());
    }

    /**
     * 验证未读列表会返回稳定的未读投影结果。
     */
    @Test
    @DisplayName("list unreads returns unread projections")
    void listUnreads_returnsUnreadProjections() {
        TestContext context = newContext();
        context.channelReadStateRepository.unreadResults = List.of(new ChannelUnread(9L, 3L, BASE_TIME));
        ChannelQueryDomainApi service = context.createQueryService();

        var result = service.listUnreads(1001L);

        assertEquals(1, result.size());
        assertEquals("9", result.getFirst().cid());
        assertEquals(3L, result.getFirst().unreadCount());
    }

    /**
     * 验证活跃成员可以读取频道成员列表，并得到成员角色与资料字段。
     */
    @Test
    @DisplayName("list channel members active member returns member results")
    void listChannelMembers_activeMember_returnsMemberResults() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelMemberRepository.save(new ChannelMember(9L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(60), null));
        context.userProfileRepository.save(profile(1001L, "carry-owner"));
        context.userProfileRepository.save(profile(1002L, "carry-member"));
        ChannelQueryDomainApi service = context.createQueryService();

        var result = service.listChannelMembers(new ListChannelMembersQuery(1001L, 9L));

        assertEquals(2, result.size());
        assertEquals(1001L, result.get(0).accountId());
        assertEquals("carry-owner", result.get(0).nickname());
        assertEquals("OWNER", result.get(0).role());
        assertEquals(1002L, result.get(1).accountId());
        assertEquals("MEMBER", result.get(1).role());
    }

    /**
     * 验证默认频道缺失时会返回不存在问题语义。
     */
    @Test
    @DisplayName("get default channel missing channel throws not found problem")
    void getDefaultChannel_missingChannel_throwsNotFoundProblem() {
        TestContext context = newContext();
        context.channelRepository.defaultChannel = null;
        ChannelAccessDomainApi service = context.createAccessService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.getDefaultChannel(new GetDefaultChannelCommand(1001L))
        );

        assertEquals("default channel does not exist", exception.getMessage());
    }
}
