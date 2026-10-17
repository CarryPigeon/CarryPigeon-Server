package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input;



/**
 * 查询入群申请列表。
 * 职责：承载读取频道申请台账所需的最小输入。
 * 边界：只表达应用层查询参数，不承载治理权限判断与结果排序规则。
 *
 * @param accountId 发起查询的账号 ID
 * @param channelId 目标频道 ID
 */
public record ListChannelApplicationsQuery(long accountId, long channelId) {
}
