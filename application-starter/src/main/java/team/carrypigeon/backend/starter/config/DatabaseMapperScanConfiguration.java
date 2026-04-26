package team.carrypigeon.backend.starter.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库 Mapper 扫描配置。
 * 职责：仅在数据库实现启用时注册 MyBatis Mapper 扫描，避免禁用数据库场景下提前创建 Mapper Bean。
 * 边界：这里只控制数据库 Mapper 的扫描开关，不承载任何业务装配逻辑。
 */
@Configuration
@MapperScan(basePackages = "team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper")
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMapperScanConfiguration {
}
