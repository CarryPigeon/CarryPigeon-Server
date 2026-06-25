package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 数据库 Mapper 扫描自动配置。
 * 职责：仅在数据库实现启用时注册 MyBatis Mapper 扫描，保持扫描职责留在 database-impl。
 * 边界：这里只控制 mapper 注册，不承载业务服务装配逻辑。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@MapperScan(
        basePackages = "team.carrypigeon.backend.infrastructure.service.database.impl.mybatis",
        annotationClass = Mapper.class
)
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMapperScanAutoConfiguration {
}
