package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 数据库服务自动配置聚合入口。
 * 职责：聚合 database-impl 内部各 feature-oriented 自动配置，保持对外稳定导入入口。
 * 边界：不直接声明具体 Bean，实现细节下沉到更小的自动配置类。
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@Import({
        DatabaseInfrastructureAutoConfiguration.class,
        DatabaseAuthServiceAutoConfiguration.class,
        DatabaseChannelServiceAutoConfiguration.class,
        DatabaseMessageServiceAutoConfiguration.class
})
public class DatabaseServiceAutoConfiguration {
}
