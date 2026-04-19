package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * 统一项目级 Jackson 序列化约定。
 * 职责：为整个应用提供稳定的 snake_case JSON 命名策略。
 * 边界：这里只处理全局序列化约定，不负责具体业务字段转换，也不承载模块特化逻辑。
 * 原因：该能力属于全局固定基础设施，应沉到 infrastructure-basic，而不是散落在启动配置中。
 */
@AutoConfiguration
public class JacksonAutoConfiguration {

    /**
     * 为 Spring Boot 默认 ObjectMapper 注入全局命名策略。
     *
     * @return Jackson 构建器定制器，统一将 JSON 字段命名调整为 snake_case
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer snakeCaseJacksonCustomizer() {
        return builder -> builder
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 创建项目统一 JSON 能力入口。
     *
     * @param objectMapper Spring Boot 管理的统一 ObjectMapper
     * @return JSON 能力对外入口
     */
    @Bean
    public JsonProvider jsonProvider(ObjectMapper objectMapper) {
        return new JsonProvider(objectMapper);
    }
}
