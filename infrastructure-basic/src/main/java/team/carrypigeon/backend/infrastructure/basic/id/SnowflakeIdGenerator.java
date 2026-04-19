package team.carrypigeon.backend.infrastructure.basic.id;

import cn.hutool.core.lang.Snowflake;

/**
 * 基于 Hutool Snowflake 的统一 ID 生成实现。
 * 职责：为整个项目提供统一雪花 ID 生成能力。
 * 边界：这里只提供基础 ID 生成，不承载具体业务编码规则。
 */
public class SnowflakeIdGenerator implements IdGenerator {

    private final Snowflake snowflake;

    public SnowflakeIdGenerator(Snowflake snowflake) {
        this.snowflake = snowflake;
    }

    @Override
    public long nextLongId() {
        return snowflake.nextId();
    }
}
