package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import lombok.Data;

/**
 * 频道发现查询投影。
 * 职责：承接 discover SQL 返回的聚合字段，不污染频道主表实体。
 * 边界：仅供 database-impl 内部 discover 读链路使用。
 */
@Data
public class ChannelDiscoverProjection {
    private Long id;
    private String name;
    private String brief;
    private String avatar;
    private Long memberCount;
    private Boolean requiresApplication;
}
