package team.carrypigeon.backend.infrastructure.basic.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.basic.InfrastructureBasics;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 基础设施门面自动配置。
 * 职责：聚合常用固定基础设施能力，减少上层模块对具体工具类的直接感知。
 */
@AutoConfiguration
public class BasicInfrastructureAutoConfiguration {

    /**
     * 创建基础设施能力门面。
     *
     * @param idGenerator 统一雪花 ID 生成器
     * @param timeProvider 统一时间访问入口
     * @param jsonProvider 统一 JSON 能力入口
     * @return 基础设施能力门面
     */
    @Bean
    public InfrastructureBasics infrastructureBasics(
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            JsonProvider jsonProvider
    ) {
        return new InfrastructureBasics(idGenerator, timeProvider, jsonProvider);
    }
}
