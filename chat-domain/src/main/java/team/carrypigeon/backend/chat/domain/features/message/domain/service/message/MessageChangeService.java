package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageChangePage;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/** 消息变更恢复；每页重新验证当前成员权限，并拒绝已超出保留窗口的恢复位置。 */
@Service
public class MessageChangeService {
    private final ChannelContextApi channels;
    private final MessageRepository messages;
    private final TransactionRunner transactions;
    private final TimeProvider clock;
    private final RealtimeServerProperties properties;

    public MessageChangeService(ChannelContextApi channels, MessageRepository messages, TransactionRunner transactions,
            TimeProvider clock, RealtimeServerProperties properties) {
        this.channels = channels;
        this.messages = messages;
        this.transactions = transactions;
        this.clock = clock;
        this.properties = properties;
    }

    /** 空位置只建立水位；有位置时按事件序号恢复，过期返回需重新获取快照的稳定错误。 */
    public MessageChangePage getChanges(long accountId, long channelId, Long afterSequence, int limit) {
        return transactions.runInTransaction(() -> {
            // 与治理保持同样的频道锁顺序，读取期间不能在授权通过后提交撤权或删除。
            channels.requireMemberContextForUpdate(channelId, accountId);
            var page = messages.findChanges(channelId, afterSequence, limit + 1);
            if (afterSequence != null) {
                if (afterSequence < 0 || afterSequence > page.latestSequence()) {
                    throw ProblemException.of(MessageProblem.CURSOR_INVALID);
                }
                if (afterSequence < page.expiredThrough() || (!page.events().isEmpty()
                        && page.events().getFirst().createdAt().isBefore(clock.nowInstant().minus(properties.getEventRetention())))) {
                    // 清理任务可能尚未追上到期数据；不能因物理记录尚存而延长恢复协议窗口。
                    throw ProblemException.of(MessageProblem.EVENT_CURSOR_EXPIRED);
                }
            }
            return page;
        });
    }
}
