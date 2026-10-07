package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;

/**
 * 频道应用层结果。
 * 职责：向协议层暴露默认频道查询所需的稳定字段。
 * 边界：不承载协议层包装逻辑。
 *
 * @param channelId 频道 ID
 * @param name 频道名称
 * @param brief 频道简介
 * @param avatar 频道头像相对路径
 * @param ownerUid 频道 owner 用户 ID
 * @param systemChannel 是否为默认频道
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelResult(
        long channelId,
        String name,
        String brief,
        String avatar,
        String ownerUid,
        boolean systemChannel,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChannelResult from(Channel channel) {
        return from(channel, channel.ownerUid());
    }

    public static ChannelResult from(Channel channel, String ownerUid) {
        return new ChannelResult(
                channel.id(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                ownerUid,
                channel.systemChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}
