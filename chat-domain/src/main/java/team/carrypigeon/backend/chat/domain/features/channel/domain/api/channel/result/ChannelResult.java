package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result;



import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;

/**
 * 频道投影。
 * 职责：向协议层暴露频道列表与详情所需的稳定字段。
 * 边界：只承载频道聚合自身的字段，不承载成员关系、禁言记录与申请记录；不承载协议包装逻辑。
 *
 * @param channelId 频道 ID
 * @param name 频道名称
 * @param brief 频道简介；可能为空字符串
 * @param avatar 频道头像相对路径；可能为空字符串
 * @param systemChannel 是否为系统频道
 * @param createdAt 频道创建时间
 * @param updatedAt 频道资料最近更新时间
 */
public record ChannelResult(
        long channelId,
        String name,
        String brief,
        String avatar,
        boolean systemChannel,
        Instant createdAt,
        Instant updatedAt
) {


    public static ChannelResult from(Channel channel) {
        return new ChannelResult(
                channel.id(),
                channel.name(),
                channel.brief(),
                channel.avatar(),
                channel.systemChannel(),
                channel.createdAt(),
                channel.updatedAt()
        );
    }
}
