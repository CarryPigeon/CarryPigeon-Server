package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

/**
 * 频道发现领域读模型。
 * 职责：表达 discover 场景下返回给上层的频道摘要信息。
 * 边界：只服务频道发现读链路，不替代频道主聚合事实模型。
 */
public record DiscoveredChannel(
        long id,
        String name,
        String brief,
        String avatar,
        long memberCount,
        boolean requiresApplication
) {
}
