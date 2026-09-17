package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据库服务配置。
 * 职责：控制 database-impl 是否装配。
 * 边界：配置类属于具体数据库实现模块，不进入 infrastructure-basic 或 chat-domain。
 *
 * @param enabled 是否启用数据库服务实现
 */
@ConfigurationProperties(prefix = "cp.infrastructure.service.database")
public record DatabaseServiceProperties(boolean enabled) {

    public DatabaseServiceProperties() {
        this(true);
    }
}
