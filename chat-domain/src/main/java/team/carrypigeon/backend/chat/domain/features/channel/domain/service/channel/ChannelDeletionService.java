package team.carrypigeon.backend.chat.domain.features.channel.domain.service.channel;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup.ChannelMessageCleanupApi;
import team.carrypigeon.backend.chat.domain.shared.transaction.TransactionRunner;

/**
 * 已删除频道的进程内异步清理。频道在请求事务提交后撤下，关联数据在清理事务中删除。
 * 任务不持久化，进程退出或清理失败后不会自动恢复。
 */
@Slf4j
@Service
public class ChannelDeletionService {
    private final ChannelApplicationRepository applications;
    private final ChannelBanRepository bans;
    private final ChannelMemberRepository members;
    private final ChannelMessageCleanupApi messages;
    private final ChannelEventNotifier notifier;
    private final TransactionRunner transactions;
    private final Executor executor;

    public ChannelDeletionService(ChannelApplicationRepository applications,
            ChannelBanRepository bans, ChannelMemberRepository members, ChannelMessageCleanupApi messages,
            ChannelEventNotifier notifier, TransactionRunner transactions,
            @Qualifier("channelDeletionExecutor") Executor executor) {
        this.applications = applications;
        this.bans = bans;
        this.members = members;
        this.messages = messages;
        this.notifier = notifier;
        this.transactions = transactions;
        this.executor = executor;
    }

    /** 在频道删除提交后将清理工作交给进程内队列。 */
    public void submit(long channelId) {
        try {
            executor.execute(() -> cleanChannel(channelId));
        } catch (RuntimeException exception) {
            // 频道删除已经提交，无法通过抛出异常撤销；记录未受理的频道供运维处理。
            log.error("Failed to submit channel cleanup, channelId={}", channelId, exception);
        }
    }

    private void cleanChannel(long channelId) {
        try {
            transactions.runInTransaction(afterCommit -> {
                // 成员关系删除后无法再确定接收者，因此在同一事务中先保存账号列表。
                List<Long> accountIds = members.listByChannelId(channelId).stream()
                        .map(ChannelMember::accountId).toList();
                applications.deleteByChannelId(channelId);
                bans.deleteByChannelId(channelId);
                members.deleteByChannelId(channelId);
                messages.deleteChannel(channelId);
                // 仅在成员关系删除提交后通知，避免客户端刷新时看到旧关系。
                if (!accountIds.isEmpty()) {
                    notifier.accountChannelsChanged(afterCommit, accountIds);
                }
            });
        } catch (RuntimeException exception) {
            log.error("Channel cleanup failed, channelId={}", channelId, exception);
        }
    }
}
