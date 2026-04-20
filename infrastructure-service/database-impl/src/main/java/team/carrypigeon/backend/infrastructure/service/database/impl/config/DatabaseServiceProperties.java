package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据库服务配置。
 * 职责：控制 database-impl 是否装配以及健康检查 SQL。
 * 边界：配置类属于具体数据库实现模块，不进入 infrastructure-basic 或 chat-domain。
 *
 * @param enabled 是否启用数据库服务实现
 * @param healthQuery 健康检查 SQL，必须是当前数据库可执行的轻量查询
 */
@ConfigurationProperties(prefix = "cp.infrastructure.service.database")
public record DatabaseServiceProperties(boolean enabled, String healthQuery) {

    public DatabaseServiceProperties {
        if (healthQuery == null || healthQuery.isBlank()) {
            healthQuery = "SELECT 1";
        }
    }

    public DatabaseServiceProperties() {
        this(true, "SELECT 1");
    }
}
