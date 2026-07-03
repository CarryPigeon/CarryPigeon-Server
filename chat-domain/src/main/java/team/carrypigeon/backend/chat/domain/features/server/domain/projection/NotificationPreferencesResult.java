package team.carrypigeon.backend.chat.domain.features.server.domain.projection;

import java.util.List;

/**
 * 通知偏好应用层结果。
 */
public record NotificationPreferencesResult(
        ServerPreferenceResult server,
        List<ChannelPreferenceResult> channels
) {
    /**
     * 服务级通知偏好领域结果。
     * 职责：承载全局通知模式和静音截止时间。
     */
    public record ServerPreferenceResult(String mode, long mutedUntil) {
    }

    /**
     * 频道级通知偏好领域结果。
     * 职责：承载单个频道上的通知模式覆盖。
     */
    public record ChannelPreferenceResult(String cid, String mode, long mutedUntil) {
    }
}
