package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 频道封禁 ChannelBan_* 相关 key。
 */
public final class CPNodeChannelBanKeys {

    /** {@code CPChannelBan}: 禁言实体。 */
    public static final CPKey<CPChannelBan> CHANNEL_BAN_INFO = CPKey.of("ChannelBanInfo", CPChannelBan.class);
    /** Long: 禁言记录 id */
    public static final CPKey<Long> CHANNEL_BAN_ID = CPKey.of("ChannelBan_Id", Long.class);
    /** Long: 频道 id */
    public static final CPKey<Long> CHANNEL_BAN_CID = CPKey.of("ChannelBan_Cid", Long.class);
    /** Long: 被封禁用户 id（与业务中 uid 字段对应） */
    public static final CPKey<Long> CHANNEL_BAN_TARGET_UID = CPKey.of("ChannelBan_TargetUid", Long.class);
    /** Long: 操作管理员 id */
    public static final CPKey<Long> CHANNEL_BAN_AID = CPKey.of("ChannelBan_Aid", Long.class);
    /** {@code Integer}: 禁言时长（seconds）。 */
    public static final CPKey<Integer> CHANNEL_BAN_DURATION = CPKey.of("ChannelBan_Duration", Integer.class);
    /** {@code Long}: 禁言截止时间（毫秒时间戳）。 */
    public static final CPKey<Long> CHANNEL_BAN_UNTIL_TIME = CPKey.of("ChannelBan_UntilTime", Long.class);
    /** {@code String}: 禁言原因（可为空）。 */
    public static final CPKey<String> CHANNEL_BAN_REASON = CPKey.of("ChannelBan_Reason", String.class);
    /** LocalDateTime: 创建时间 */
    public static final CPKey<LocalDateTime> CHANNEL_BAN_CREATE_TIME = CPKey.of("ChannelBan_CreateTime", LocalDateTime.class);
    /** {@code List<CPChannelBan>}: 禁言列表。 */
    public static final CPKey<List> CHANNEL_BAN_ITEMS = CPKey.of("ChannelBanItems", List.class);

    private CPNodeChannelBanKeys() {
    }
}
