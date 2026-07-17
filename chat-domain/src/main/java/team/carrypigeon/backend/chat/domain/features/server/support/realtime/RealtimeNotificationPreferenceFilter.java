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
     * 创建不过滤接收人的实例。
     * 用途：测试或显式跳过通知偏好过滤的发布器装配。
     *
     * @return 允许全部接收人的过滤器实例
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

    /**
     * 判断单个账号是否允许接收指定事件。
     * 规则：频道偏好优先；频道为 inherit 或不存在时回落到账户级偏好。
     *
     * @param accountId 接收账号 ID
     * @param channelId 事件所属频道 ID，可为空
     * @param eventType realtime event 类型
     * @return 允许接收时返回 true
     */
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

    /**
     * 查找账号在指定频道上的通知偏好。
     * 约束：无频道上下文的事件不能使用频道级偏好。
     *
     * @param accountId 接收账号 ID
     * @param channelId 事件所属频道 ID
     * @return 频道级通知偏好
     */
    private Optional<NotificationChannelPreference> findChannelPreference(long accountId, Long channelId) {
        if (channelId == null) {
            return Optional.empty();
        }
        return notificationPreferenceRepository.listChannelPreferencesByAccountId(accountId).stream()
                .filter(preference -> preference.channelId() == channelId)
                .findFirst();
    }

    /**
     * 按通知模式判断事件是否可投递。
     * 语义：`mentions_only` 只允许 mention 事件，`muted` 在静音有效期内阻断事件。
     *
     * @param mode 通知模式
     * @param mutedUntil 静音截止毫秒时间戳，0 表示长期静音
     * @param eventType realtime event 类型
     * @return 当前模式允许投递时返回 true
     */
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

    /**
     * 判断静音设置当前是否生效。
     * 语义：0 表示长期静音；大于当前时间表示临时静音仍在有效期内。
     *
     * @param mutedUntil 静音截止毫秒时间戳
     * @return 静音仍生效时返回 true
     */
    private boolean isMuteActive(long mutedUntil) {
        return mutedUntil == 0L || mutedUntil > timeProvider.nowMillis();
    }
}
