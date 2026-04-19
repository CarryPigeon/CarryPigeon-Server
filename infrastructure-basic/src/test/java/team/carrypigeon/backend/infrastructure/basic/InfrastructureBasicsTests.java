package team.carrypigeon.backend.infrastructure.basic;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.id.SnowflakeIdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 验证基础设施门面的最小契约。
 * 职责：确保上层模块可通过一个简洁入口访问 ID、时间、JSON 三类固定基建能力。
 * 边界：不验证各基础能力内部行为。
 */
class InfrastructureBasicsTests {

    /**
     * 测试门面能力暴露。
     * 输入：ID、时间、JSON 三个基础能力实例。
     * 期望：门面按原实例暴露，避免上层模块感知装配细节。
     */
    @Test
    void accessors_createdWithProviders_returnSameProviders() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(new Snowflake(1, 1));
        TimeProvider timeProvider = new TimeProvider(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        JsonProvider jsonProvider = new JsonProvider(new ObjectMapper());

        InfrastructureBasics infrastructureBasics = new InfrastructureBasics(idGenerator, timeProvider, jsonProvider);

        assertSame(idGenerator, infrastructureBasics.ids());
        assertSame(timeProvider, infrastructureBasics.time());
        assertSame(jsonProvider, infrastructureBasics.json());
    }
}
