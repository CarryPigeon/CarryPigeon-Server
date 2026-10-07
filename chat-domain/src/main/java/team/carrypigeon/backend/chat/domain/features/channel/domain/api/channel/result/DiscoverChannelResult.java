package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.DiscoveredChannel;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

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

    public static DiscoverChannelResult from(DiscoveredChannel channel) {
        return new DiscoverChannelResult(
                IdUtil.toString(channel.id()),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                channel.memberCount(),
                channel.requiresApplication()
        );
    }
}
