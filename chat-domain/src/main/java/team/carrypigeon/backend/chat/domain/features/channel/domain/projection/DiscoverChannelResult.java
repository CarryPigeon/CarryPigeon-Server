package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

/**
 * 远端频道发现结果。
 */
public record DiscoverChannelResult(
        String cid,
        String name,
        String brief,
        String avatar,
        long memberCount,
        boolean requiresApplication
) {
}
