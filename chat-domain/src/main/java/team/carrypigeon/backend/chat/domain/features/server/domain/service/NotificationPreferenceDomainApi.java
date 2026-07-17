package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.NotificationPreferenceApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationChannelPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.UpdateNotificationServerPreferenceCommand;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.NotificationPreferencesResult;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationChannelPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.model.NotificationServerPreference;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 通知偏好领域服务。
 */
@Service
public class NotificationPreferenceDomainApi implements NotificationPreferenceApi {

    private static final List<String> SERVER_MODES = List.of("all", "mentions_only", "muted");
    private static final List<String> CHANNEL_MODES = List.of("all", "mentions_only", "muted", "inherit");

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final TimeProvider timeProvider;

    public NotificationPreferenceDomainApi(
            NotificationPreferenceRepository notificationPreferenceRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            TimeProvider timeProvider
    ) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * 查询账号级通知偏好聚合。
     * 输入：账号标识。
     * 输出：服务端默认偏好与频道级覆盖项。
     *
     * @param accountId 账号标识
     * @return 通知偏好结果
     */
    public NotificationPreferencesResult getNotificationPreferences(long accountId) {
        requirePositive(accountId, "accountId");
        NotificationPreferencesResult.ServerPreferenceResult server = notificationPreferenceRepository.findServerPreferenceByAccountId(accountId)
                .map(preference -> new NotificationPreferencesResult.ServerPreferenceResult(preference.mode(), preference.mutedUntil()))
                .orElseGet(() -> new NotificationPreferencesResult.ServerPreferenceResult("all", 0L));
        List<NotificationPreferencesResult.ChannelPreferenceResult> channels = notificationPreferenceRepository.listChannelPreferencesByAccountId(accountId).stream()
                .map(preference -> new NotificationPreferencesResult.ChannelPreferenceResult(Ids.toString(preference.channelId()), preference.mode(), preference.mutedUntil()))
                .toList();
        return new NotificationPreferencesResult(server, channels);
    }

    /**
     * 更新服务端级通知偏好。
     * 输入：账号标识、偏好模式与静音截止时间。
     * 副作用：对目标账号执行 upsert。
     *
     * @param command 服务端通知偏好更新命令
     */
    public void updateServerPreference(UpdateNotificationServerPreferenceCommand command) {
        requirePositive(command.accountId(), "accountId");
        String mode = normalizeMode(command.mode(), SERVER_MODES, "mode");
        long mutedUntil = normalizeMutedUntil(command.mutedUntil());
        NotificationServerPreference existing = notificationPreferenceRepository.findServerPreferenceByAccountId(command.accountId()).orElse(null);
        notificationPreferenceRepository.upsertServerPreference(new NotificationServerPreference(
                command.accountId(),
                mode,
                mutedUntil,
                existing == null ? timeProvider.nowInstant() : existing.createdAt(),
                timeProvider.nowInstant()
        ));
    }

    /**
     * 更新频道级通知偏好。
     * 输入：账号标识、频道标识、偏好模式与静音截止时间。
     * 约束：只有频道成员才能覆盖频道级通知偏好。
     * 副作用：对目标账号和频道执行 upsert。
     *
     * @param command 频道通知偏好更新命令
     */
    public void updateChannelPreference(UpdateNotificationChannelPreferenceCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        channelRepository.findById(command.channelId()).orElseThrow(() -> ProblemException.notFound("channel does not exist"));
        if (!channelMemberRepository.exists(command.channelId(), command.accountId())) {
            throw ProblemException.forbidden("not_channel_member", "channel membership is required");
        }
        String mode = normalizeMode(command.mode(), CHANNEL_MODES, "mode");
        long mutedUntil = normalizeMutedUntil(command.mutedUntil());
        NotificationChannelPreference existing = notificationPreferenceRepository.listChannelPreferencesByAccountId(command.accountId()).stream()
                .filter(preference -> preference.channelId() == command.channelId())
                .findFirst()
                .orElse(null);
        notificationPreferenceRepository.upsertChannelPreference(new NotificationChannelPreference(
                command.accountId(),
                command.channelId(),
                mode,
                mutedUntil,
                existing == null ? timeProvider.nowInstant() : existing.createdAt(),
                timeProvider.nowInstant()
        ));
    }

    /**
     * 规范化通知模式。
     * 约束：不同入口传入各自允许的模式集合，非法模式统一返回字段校验失败。
     *
     * @param rawMode 客户端提交的通知模式
     * @param allowedModes 当前入口允许的模式集合
     * @param fieldName 字段名
     * @return 规范化后的通知模式
     */
    private String normalizeMode(String rawMode, List<String> allowedModes, String fieldName) {
        if (rawMode == null || rawMode.isBlank()) {
            throw ProblemException.validationFailed(fieldName + " must not be blank");
        }
        String mode = rawMode.trim();
        if (!allowedModes.contains(mode)) {
            throw ProblemException.validationFailed(fieldName + " is invalid");
        }
        return mode;
    }

    /**
     * 规范化静音截止时间。
     * 语义：缺失值转换为 0，表示长期静音或无单独截止时间，由通知模式决定实际含义。
     *
     * @param mutedUntil 客户端提交的静音截止毫秒时间戳
     * @return 规范化后的静音截止毫秒时间戳
     */
    private long normalizeMutedUntil(Long mutedUntil) {
        if (mutedUntil == null) {
            return 0L;
        }
        if (mutedUntil < 0L) {
            throw ProblemException.validationFailed("muted_until must be greater than or equal to 0");
        }
        return mutedUntil;
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}
