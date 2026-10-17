package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input;



/**
 * 频道发现查询。
 * 职责：承载频道发现用例的关键字、游标与分页上限。
 * 边界：只表达应用层查询参数，不承载可见性过滤与分页结果判定规则。
 *
 * @param accountId 发起发现的账号 ID
 * @param keyword 频道名称或简介关键字；为空表示不按关键字过滤
 * @param cursorChannelId 游标频道 ID；为空表示从第一页开始
 * @param limit 单页返回条数上限
 */
public record DiscoverChannelsQuery(long accountId, String keyword, Long cursorChannelId, int limit) {
}
