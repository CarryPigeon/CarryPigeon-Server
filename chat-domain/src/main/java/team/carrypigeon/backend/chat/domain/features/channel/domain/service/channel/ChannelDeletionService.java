package team.carrypigeon.backend.chat.domain.features.channel.domain.service.channel;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelApplicationRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.cleanup.ChannelMessageCleanupApi;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 已删除频道的分批清理任务。频道在请求事务中已撤下，本服务只清理关联数据。
 * 每批独立提交并持有删除任务行锁；异常回滚当前批，重启后按未完成记录继续，不把进度仅放在内存中。
 */
@Slf4j
@Service
public class ChannelDeletionService {
    private static final int BATCH_SIZE = 1000;
    private final ChannelRepository channels;
    private final ChannelApplicationRepository applications;
    private final ChannelBanRepository bans;
    private final ChannelMemberRepository members;
    private final ChannelMessageCleanupApi messages;
    private final ChannelEventNotifier notifier;
    private final TransactionRunner transactions;

    public ChannelDeletionService(ChannelRepository channels, ChannelApplicationRepository applications,
            ChannelBanRepository bans, ChannelMemberRepository members, ChannelMessageCleanupApi messages,
            ChannelEventNotifier notifier, TransactionRunner transactions) {
        this.channels = channels;
        this.applications = applications;
        this.bans = bans;
        this.members = members;
        this.messages = messages;
        this.notifier = notifier;
        this.transactions = transactions;
    }

    /** 每次调度只处理一批，避免大频道清理长期占用数据库事务或调度线程。 */
    @Scheduled(fixedDelayString = "${cp.channel.deletion-cleanup-delay-ms:1000}")
    public void cleanPendingChannels() {
        try {
            cleanNextBatch();
        } catch (RuntimeException exception) {
            // 当前批已回滚，任务仍是未完成状态；后续调度会重试，不能误标完成。
            log.error("Channel deletion batch failed; pending task will be retried", exception);
        }
    }

    /** 处理一个持锁任务；返回 false 表示没有待处理任务，便于手动运维时逐批推进。 */
    public boolean cleanNextBatch() {
        return transactions.runInTransaction(afterCommit -> {
            Long channelId = channels.findPendingDeletionForUpdate().orElse(null);
            if (channelId == null) {
                return false;
            }
            int applicationsDeleted = applications.deleteBatchByChannelId(channelId, BATCH_SIZE);
            int bansDeleted = bans.deleteBatchByChannelId(channelId, BATCH_SIZE);
            List<Long> removedAccounts = members.deleteBatchByChannelId(channelId, BATCH_SIZE);
            boolean messagesDone = messages.deleteChannelBatch(channelId, BATCH_SIZE);
            if (!removedAccounts.isEmpty()) {
                notifier.accountChannelsChanged(afterCommit, removedAccounts);
            }
            if (applicationsDeleted == 0 && bansDeleted == 0 && removedAccounts.isEmpty() && messagesDone) {
                channels.completeDeletion(channelId);
            }
            return true;
        });
    }
}
