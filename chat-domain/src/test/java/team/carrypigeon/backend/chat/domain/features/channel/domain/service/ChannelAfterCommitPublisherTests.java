package team.carrypigeon.backend.chat.domain.features.channel.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 频道提交后事件发布契约测试。
 * 职责：验证 channel feature 只发布自身拥有的事实事件，并保留接收者快照与默认范围。
 */
@Tag("contract")
class ChannelAfterCommitPublisherTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");
    private static final AfterCommitExecutor DIRECT_AFTER_COMMIT = Runnable::run;

    /**
     * 验证频道变化使用默认范围并复制接收账号快照。
     */
    @Test
    @DisplayName("publish channel changed blank scope emits channel fact")
    void publishChannelChangedAfterCommit_blankScope_emitsChannelFact() {
        List<Object> events = new ArrayList<>();
        ChannelAfterCommitPublisher publisher = new ChannelAfterCommitPublisher(events::add);
        List<Long> recipients = new ArrayList<>(List.of(1001L, 1002L));
        Channel channel = new Channel(9L, 9L, "general", "", "", "1001", "private", false,
                BASE_TIME, BASE_TIME);

        publisher.publishChannelChangedAfterCommit(DIRECT_AFTER_COMMIT, channel, " ", recipients);
        recipients.clear();

        ChannelChangedEvent event = assertInstanceOf(ChannelChangedEvent.class, events.getFirst());
        assertEquals(9L, event.channelId());
        assertEquals("profile", event.scope());
        assertEquals(List.of(1001L, 1002L), event.recipientAccountIds());
    }

    /**
     * 验证账号频道集合变化只携带目标账号，不泄漏 realtime 命令类型。
     */
    @Test
    @DisplayName("publish channels changed account emits account fact")
    void publishChannelsChangedAfterCommit_account_emitsAccountFact() {
        List<Object> events = new ArrayList<>();
        ChannelAfterCommitPublisher publisher = new ChannelAfterCommitPublisher(events::add);

        publisher.publishChannelsChangedAfterCommit(DIRECT_AFTER_COMMIT, 1002L);

        AccountChannelsChangedEvent event = assertInstanceOf(AccountChannelsChangedEvent.class, events.getFirst());
        assertEquals(1002L, event.accountId());
    }
}
