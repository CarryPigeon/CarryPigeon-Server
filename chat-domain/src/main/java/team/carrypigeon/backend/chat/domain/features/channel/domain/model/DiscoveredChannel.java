package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

/**
 * 频道发现领域读模型。
 * 职责：表达发现场景下返回给调用方的频道摘要，包含成员规模与加入门槛提示。
 * 边界：只服务频道发现读链路，不是频道主聚合事实模型；不承载成员关系、禁言与申请状态，字段增减不影响 Channel 聚合。
 *
 * @param id 频道 ID
 * @param name 频道名称
 * @param brief 频道简介；允许空字符串
 * @param avatar 频道头像相对路径；允许空字符串
 * @param memberCount 频道当前成员数量
 * @param requiresApplication 加入该频道是否需要先提交申请
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
