package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.DiscoveredChannel;

/**
 * 频道发现结果投影。
 * 职责：向协议层暴露发现列表所需的频道摘要字段。
 * 边界：只服务发现读链路，不承载频道完整资料、成员关系与禁言状态。
 *
 * @param cid 频道 ID
 * @param name 频道名称
 * @param brief 频道简介；可能为空字符串
 * @param avatar 频道头像相对路径；可能为空字符串
 * @param memberCount 频道当前成员数量
 * @param requiresApplication 加入该频道是否需要先提交申请
 */
public record DiscoverChannelResult(
        long cid,
        String name,
        String brief,
        String avatar,
        long memberCount,
        boolean requiresApplication
) {

    /**
     * 由发现读模型构造投影。
     * 说明：纯字段映射，不补算成员数或加入门槛，取值完全来自仓储侧聚合结果。
     *
     * @param channel 频道发现读模型
     * @return 频道发现结果投影
     */
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
