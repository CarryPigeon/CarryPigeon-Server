package team.carrypigeon.backend.chat.domain.features.channel.adaptor.realtime;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelEventNotifier;
import team.carrypigeon.backend.chat.domain.features.channel.domain.capability.ChannelChangeScope;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 基于实时通道的频道变化通知适配器。
 * 职责：把域内的变化通知翻译为线上事件名与载荷，并在事务提交后交给实时通道发布。
 * 边界：线上事件名、字段类型与取值约正则全部收敛在本类与其载荷记录中。
 */
@Component
public class RealtimeChannelEventNotifier implements ChannelEventNotifier {

    private static final String REFRESH_HINT = "refresh";
    private static final RealtimeEventType CHANNEL_CHANGED = new RealtimeEventType("channel.changed");
    private static final RealtimeEventType CHANNELS_CHANGED = new RealtimeEventType("channels.changed");

    private final RealtimeEventApi realtimeEventApi;

    public RealtimeChannelEventNotifier(RealtimeEventApi realtimeEventApi) {
        this.realtimeEventApi = realtimeEventApi;
    }

    @Override
    public void channelChanged(
            AfterCommitExecutor afterCommit,
            long channelId,
            ChannelChangeScope scope,
            Collection<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                CHANNEL_CHANGED,
                new ChannelChangedPayload(Long.toString(channelId), wireScope(scope), REFRESH_HINT),
                List.copyOf(recipientAccountIds)
        )));
    }

    @Override
    public void accountChannelsChanged(AfterCommitExecutor afterCommit, List<Long> accountIds) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                CHANNELS_CHANGED,
                new ChannelsChangedPayload(REFRESH_HINT),
                accountIds
        )));
    }

    /**
     * 把变化范围翻译为线上取值。
     * 约束：缺失范围按资料变化处理，取值使用枚举名的小写形式。
     *
     * @param scope 变化范围，可为空
     * @return 线上取值
     */
    private String wireScope(ChannelChangeScope scope) {
        return (scope == null ? ChannelChangeScope.PROFILE : scope).name().toLowerCase(Locale.ROOT);
    }
}
