package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.CreatePrivateChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.DeleteChannelCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.UpdateChannelProfileCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.RollbackingTransactionRunner;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.TestContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.BASE_TIME;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.newContext;
import static team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelDomainApiTestSupport.privateChannel;

/**
 * 频道生命周期契约测试。
 * 职责：验证频道创建、更新与删除链路的应用层编排语义。
 * 边界：不验证 HTTP 协议层与真实数据库访问，只使用内存替身验证业务语义。
 */
@Tag("contract")
class ChannelDomainApiLifecycleTests {

    /**
     * 验证创建 private channel 时会写入频道记录，并把创建者登记为 OWNER。
     */
    @Test
    @DisplayName("create private channel valid command saves private channel and owner membership")
    void createPrivateChannel_validCommand_savesPrivateChannelAndOwnerMembership() {
        TestContext context = newContext();
        ChannelLifecycleDomainApi service = context.createLifecycleService();

        var result = service.createPrivateChannel(new CreatePrivateChannelCommand(1001L, "engineering"));

        assertEquals(2001L, result.channelId());
        assertEquals(2001L, result.conversationId());
        assertEquals("private", result.type());
        assertEquals("", result.brief());
        assertEquals(false, result.defaultChannel());
        assertNotNull(context.channelRepository.savedChannel);
        ChannelMember ownerMember = context.channelMemberRepository.findByChannelIdAndAccountId(result.channelId(), 1001L).orElseThrow();
        assertEquals(ChannelMemberRole.OWNER, ownerMember.role());
        assertTrue(context.channelRealtimePublisher.channelsChangedAccountIds.contains(1001L));
    }

    /**
     * 验证事务失败时，已登记的频道变更广播不会提前发出。
     */
    @Test
    @DisplayName("create private channel rollback skips realtime publish")
    void createPrivateChannel_rollback_skipsRealtimePublish() {
        TestContext context = newContext();
        ChannelLifecycleDomainApi service = context.createLifecycleService(new RollbackingTransactionRunner());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createPrivateChannel(new CreatePrivateChannelCommand(1001L, "engineering"))
        );

        assertEquals("transaction rolled back", exception.getMessage());
        assertEquals(0, context.channelRealtimePublisher.channelsChangedAccountIds.size());
    }

    /**
     * 验证 OWNER 更新频道资料时会同步持久化名称和简介。
     */
    @Test
    @DisplayName("update channel profile owner updates channel name and brief")
    void updateChannelProfile_owner_updatesChannelNameAndBrief() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        ChannelLifecycleDomainApi service = context.createLifecycleService();

        var result = service.updateChannelProfile(new UpdateChannelProfileCommand(1001L, 9L, "project-beta", "新的简介"));

        assertEquals("project-beta", result.name());
        assertEquals("新的简介", result.brief());
        assertEquals("project-beta", context.channelRepository.channels.get(9L).name());
        assertEquals("新的简介", context.channelRepository.channels.get(9L).brief());
        assertTrue(context.channelRealtimePublisher.channelChangedScopes.contains("profile"));
    }

    /**
     * 验证 OWNER 可以删除频道。
     */
    @Test
    @DisplayName("delete channel owner removes channel")
    void deleteChannel_owner_removesChannel() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        ChannelLifecycleDomainApi service = context.createLifecycleService();

        service.deleteChannel(new DeleteChannelCommand(1001L, 9L));

        assertEquals(false, context.channelRepository.channels.containsKey(9L));
    }

    /**
     * 验证存在审计日志依赖时会拒绝删除频道。
     */
    @Test
    @DisplayName("delete channel with dependent audit logs throws conflict problem")
    void deleteChannel_withDependentAuditLogs_throwsConflictProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelAuditLogRepository.append(new ChannelAuditLog(
                8001L,
                9L,
                1001L,
                "CHANNEL_UPDATED",
                null,
                "{}",
                BASE_TIME
        ));
        ChannelLifecycleDomainApi service = context.createLifecycleService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.deleteChannel(new DeleteChannelCommand(1001L, 9L))
        );

        assertEquals("channel contains dependent data and cannot be deleted", exception.getMessage());
        assertEquals("channel_delete_blocked", exception.reason());
        assertTrue(context.channelRepository.channels.containsKey(9L));
    }

    /**
     * 验证底层删除阶段的运行时依赖冲突会被稳定映射为 409，而不是冒泡成 500。
     */
    @Test
    @DisplayName("delete channel runtime delete failure throws conflict problem")
    void deleteChannel_runtimeDeleteFailure_throwsConflictProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.channelRepository.deleteFailure = new IllegalStateException("fk constraint");
        ChannelLifecycleDomainApi service = context.createLifecycleService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.deleteChannel(new DeleteChannelCommand(1001L, 9L))
        );

        assertEquals("channel contains dependent data and cannot be deleted", exception.getMessage());
        assertEquals("channel_delete_blocked", exception.reason());
        assertTrue(context.channelRepository.channels.containsKey(9L));
    }

    /**
     * 验证存在消息依赖时会拒绝删除频道。
     */
    @Test
    @DisplayName("delete channel with dependent messages throws conflict problem")
    void deleteChannel_withDependentMessages_throwsConflictProblem() {
        TestContext context = newContext();
        context.channelRepository.channels.put(9L, privateChannel(9L, "project-alpha"));
        context.channelMemberRepository.save(new ChannelMember(9L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null));
        context.messageRepository.save(new ChannelMessage(
                5001L,
                "550e8400-e29b-41d4-a716-446655440000",
                9L,
                9L,
                1001L,
                "text",
                "hello",
                "hello",
                "hello",
                null,
                null,
                "sent",
                BASE_TIME
        ));
        ChannelLifecycleDomainApi service = context.createLifecycleService();

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> service.deleteChannel(new DeleteChannelCommand(1001L, 9L))
        );

        assertEquals("channel contains dependent data and cannot be deleted", exception.getMessage());
        assertEquals("channel_delete_blocked", exception.reason());
        assertTrue(context.channelRepository.channels.containsKey(9L));
    }
}
