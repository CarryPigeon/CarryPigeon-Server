package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.MessageChangeApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.input.MessageChangeCursor;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.change.result.MessageChangePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageChangeReader;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/** 频道短期事件恢复；每次读取都重新检查当前成员权限。 */
@Service
public class MessageChangeService implements MessageChangeApi {
    private final ChannelContextApi channels;
    private final MessageChangeReader events;
    private final TransactionRunner transactions;

    public MessageChangeService(ChannelContextApi channels, MessageChangeReader events, TransactionRunner transactions) {
        this.channels = channels;
        this.events = events;
        this.transactions = transactions;
    }

    @Override
    public MessageChangePage getChanges(long accountId, long channelId, MessageChangeCursor cursor, int limit) {
        return transactions.runInTransaction(() -> {
            // 与频道治理保持锁顺序，避免通过权限检查后并发撤销成员关系。
            channels.requireMemberContextForUpdate(channelId, accountId);
            return events.read(channelId, cursor, limit);
        });
    }
}
