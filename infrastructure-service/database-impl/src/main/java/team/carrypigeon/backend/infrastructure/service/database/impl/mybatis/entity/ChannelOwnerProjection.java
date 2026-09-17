package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import lombok.Data;

/** 频道 owner 批量查询的内部投影。 */
@Data
public class ChannelOwnerProjection {

    private Long channelId;
    private Long accountId;
}
