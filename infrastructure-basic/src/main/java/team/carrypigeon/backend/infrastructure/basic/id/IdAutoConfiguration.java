package team.carrypigeon.backend.infrastructure.basic.id;

import cn.hutool.core.lang.Snowflake;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 统一 ID 基础设施自动配置。
 * 职责：基于配置创建全局雪花 ID 生成器。
 * 边界：这里只配置基础 ID 生成，不处理任何业务 ID 语义。
 */
@AutoConfiguration
@EnableConfigurationProperties(SnowflakeIdProperties.class)
public class IdAutoConfiguration {

    /**
     * 根据项目配置创建 Hutool Snowflake 实例。
     *
     * @param properties 雪花 ID 节点配置
     * @return 统一雪花 ID 生成器
     */
    @Bean
    public IdGenerator idGenerator(SnowflakeIdProperties properties) {
        Snowflake snowflake = new Snowflake(properties.workerId(), properties.datacenterId());
        return new SnowflakeIdGenerator(snowflake);
    }
}
