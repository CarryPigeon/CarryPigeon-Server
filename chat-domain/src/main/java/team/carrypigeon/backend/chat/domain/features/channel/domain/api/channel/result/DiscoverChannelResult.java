package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.DiscoveredChannel;

/**
 * 远端频道发现结果。
 */
public record DiscoverChannelResult(
        long cid,
        String name,
        String brief,
        String avatar,
        long memberCount,
        boolean requiresApplication
) {

    public static DiscoverChannelResult from(DiscoveredChannel channel) {
        return new DiscoverChannelResult(
            channel.id(),
            channel.name(),
            channel.brief(),
            channel.avatar(),
            channel.memberCount(),
            channel.requiresApplication()
        );
    }
}
