package team.carrypigeon.backend.infrastructure.service.database.api.model;

/**
 * 频道发现读侧数据库记录契约。
 * 职责：表达 discover 查询返回的频道摘要投影。
 * 边界：仅服务 discover 读侧，不混入频道主事实写模型。
 *
 * @param id 频道 ID
 * @param name 频道名称
 * @param brief 频道简介
 * @param avatar 频道头像相对路径
 * @param memberCount 成员数
 * @param requiresApplication 是否需要申请加入
 */
public record ChannelDiscoverRecord(
        long id,
        String name,
        String brief,
        String avatar,
        long memberCount,
        boolean requiresApplication
) {
}
