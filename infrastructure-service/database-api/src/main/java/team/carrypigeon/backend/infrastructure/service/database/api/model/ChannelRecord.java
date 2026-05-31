package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道数据库记录契约。
 * 职责：表达频道读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载额外业务语义。
 *
 * @param id 频道 ID
 * @param conversationId 会话 ID
 * @param name 频道名称
 * @param brief 频道简介
 * @param avatar 频道头像相对路径
 * @param type 频道类型
 * @param defaultChannel 是否为默认频道
 * @param memberCount 成员数
 * @param requiresApplication 是否需要申请加入
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelRecord(
        long id,
        long conversationId,
        String name,
        String brief,
        String avatar,
        String type,
        boolean defaultChannel,
        long memberCount,
        boolean requiresApplication,
        Instant createdAt,
        Instant updatedAt
) {

    public ChannelRecord(
            long id,
            long conversationId,
            String name,
            String brief,
            String avatar,
            String type,
            boolean defaultChannel,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, conversationId, name, brief, avatar, type, defaultChannel, 0L, false, createdAt, updatedAt);
    }
}
