package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时通知偏好过滤器。
 * 职责：在 Netty 事件写入缓存和推送前，按账号级与频道级通知偏好筛选接收人。
 * 边界：只处理通知类 realtime event，不处理连接认证、ping、replay 和频道列表结构同步。
 */
public class RealtimeNotificationPreferenceFilter {

    private static final Set<String> FILTERED_EVENT_TYPES = Set.of(
            "message.created",
            "message.updated",
            "message.recalled",
            "message.deleted",
            "message.pinned",
            "message.unpinned",
            "mention.created"
    );

    private static final RealtimeNotificationPreferenceFilter ALLOW_ALL = new RealtimeNotificationPreferenceFilter();

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final TimeProvider timeProvider;
    private final boolean allowAll;

    public RealtimeNotificationPreferenceFilter(
            NotificationPreferenceRepository notificationPreferenceRepository,
            TimeProvider timeProvider
    ) {
        this.notificationPreferenceRepository = Objects.requireNonNull(notificationPreferenceRepository, "notificationPreferenceRepository");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider");
        this.allowAll = false;
    }

    private RealtimeNotificationPreferenceFilter() {
        this.notificationPreferenceRepository = null;
        this.timeProvider = null;
        this.allowAll = true;
    }

    /**
     * 创建不过滤接收人的测试 / 兼容实例。
     *
     * @return 保留旧发布器构造契约的允许全部实例
     */
    public static RealtimeNotificationPreferenceFilter allowAll() {
        return ALLOW_ALL;
    }

    /**
     * 按通知偏好筛选目标接收人。
     * 输入：事件所属频道、事件类型和候选接收账号。
     * 输出：仍允许接收该事件的账号列表；返回空列表时发布器应跳过缓存写入与推送。
     *
     * @param channelId 事件所属频道；为空时只按账号级偏好判断
     * @param eventType realtime event 类型
     * @param recipientAccountIds 候选接收账号集合
     * @return 过滤后的接收账号列表
     */
    public List<Long> filterRecipients(Long channelId, String eventType, Collection<Long> recipientAccountIds) {
        if (recipientAccountIds == null || recipientAccountIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedRecipients = recipientAccountIds.stream()
                .filter(Objects::nonNull)
                .toList();
        if (allowAll || eventType == null || !FILTERED_EVENT_TYPES.contains(eventType)) {
            return normalizedRecipients;
        }
        List<Long> allowedRecipients = new ArrayList<>();
        for (Long accountId : normalizedRecipients) {
            if (allows(accountId, channelId, eventType)) {
                allowedRecipients.add(accountId);
            }
        }
        return List.copyOf(allowedRecipients);
    }

    private boolean allows(long accountId, Long channelId, String eventType) {
        Optional<NotificationChannelPreference> channelPreference = findChannelPreference(accountId, channelId);
        if (channelPreference.isPresent() && !"inherit".equals(channelPreference.get().mode())) {
            return allowsMode(channelPreference.get().mode(), channelPreference.get().mutedUntil(), eventType);
        }
        NotificationServerPreference serverPreference = notificationPreferenceRepository.findServerPreferenceByAccountId(accountId).orElse(null);
        if (serverPreference == null) {
            return true;
        }
        return allowsMode(serverPreference.mode(), serverPreference.mutedUntil(), eventType);
    }

    private Optional<NotificationChannelPreference> findChannelPreference(long accountId, Long channelId) {
        if (channelId == null) {
            return Optional.empty();
        }
        return notificationPreferenceRepository.listChannelPreferencesByAccountId(accountId).stream()
                .filter(preference -> preference.channelId() == channelId)
                .findFirst();
    }

    private boolean allowsMode(String mode, long mutedUntil, String eventType) {
        if ("all".equals(mode)) {
            return true;
        }
        if ("mentions_only".equals(mode)) {
            return "mention.created".equals(eventType);
        }
        if ("muted".equals(mode)) {
            return !isMuteActive(mutedUntil);
        }
        return true;
    }

    private boolean isMuteActive(long mutedUntil) {
        return mutedUntil == 0L || mutedUntil > timeProvider.nowMillis();
    }
}
