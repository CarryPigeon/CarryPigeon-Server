package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

/**
 * 频道变化范围。
 * 职责：表达频道内哪一类视图已发生变化，供通知侧决定需要刷新什么。
 * 边界：只表达变化类别，不携带线上取值、序列化注解或具体数据。
 */
public enum ChannelChangeScope {

    /** 频道资料变化。 */
    PROFILE,

    /** 频道成员集合变化。 */
    MEMBERS,

    /** 入群申请集合变化。 */
    APPLICATIONS,

    /** 禁言台账变化。 */
    BANS
}
