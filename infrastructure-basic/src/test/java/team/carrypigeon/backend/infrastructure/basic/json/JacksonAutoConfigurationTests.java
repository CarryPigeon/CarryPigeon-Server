package team.carrypigeon.backend.infrastructure.basic.json;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JacksonAutoConfiguration 契约测试。
 * 职责：验证全局 JSON 命名与时间序列化规则。
 * 边界：只验证项目配置结果，不覆盖 Jackson 内部实现细节。
 */
@Tag("contract")
class JacksonAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration.class
            ));

    /**
     * 验证 ObjectMapper 会输出 snake_case 字段。
     */
    @Test
    void objectMapper_serializesSnakeCaseFieldNames() {
        contextRunner.run(context -> {
            JsonProvider jsonProvider = context.getBean(JsonProvider.class);
            String json = jsonProvider.toJson(new SnakeCaseProbe("carry-user"));

            assertThat(json).contains("\"display_name\":\"carry-user\"");
        });
    }

    /**
     * 验证 Instant 会序列化为 epoch 毫秒。
     */
    @Test
    void objectMapper_serializesInstantAsEpochMillis() {
        contextRunner.run(context -> {
            JsonProvider jsonProvider = context.getBean(JsonProvider.class);
            String json = jsonProvider.toJson(new InstantProbe(Instant.parse("2026-04-22T00:00:00Z")));

            assertThat(json).isEqualTo("{\"created_at\":1776816000000}");
        });
    }

    /**
     * `SnakeCaseProbe` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private record SnakeCaseProbe(String displayName) {
    }

    /**
     * `InstantProbe` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private record InstantProbe(Instant createdAt) {
    }
}
