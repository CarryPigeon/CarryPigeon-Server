package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道领域模型。
 * 职责：表达频道聚合的稳定事实，即标识、展示资料、系统频道标记与时间戳。
 * 边界：只承载已持久化的频道状态，不承载成员关系、禁言记录与申请记录；频道可见性与修改权限均由成员关系决定，不在本模型内表达。
 *
 * @param id 频道 ID
 * @param name 频道名称；创建与更新时由领域服务去除首尾空白
 * @param brief 频道简介；允许空字符串，不允许为 {@code null}
 * @param systemChannel 是否为系统频道；系统频道仅在账号初始化时自动建立成员关系，不参与成员治理
 * @param createdAt 频道创建时间
 * @param updatedAt 频道资料最近更新时间
 */
public record Channel(
        long id,
        String name,
        String brief,
        boolean systemChannel,
        Instant createdAt,
        Instant updatedAt
) {
}
