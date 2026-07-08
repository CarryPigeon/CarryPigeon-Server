package team.carrypigeon.backend.starter.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * application.yaml 默认值契约测试。
 * 职责：锁定启动装配层对外可见的关键默认配置，避免本地运行语义被无意回退。
 * 边界：只检查启动资源文本中的稳定默认值，不启动 Spring 容器。
 */
@Tag("contract")
class ApplicationYamlDefaultsTests {

    /**
     * 验证未显式配置环境变量时 realtime 默认启用。
     */
    @Test
    @DisplayName("application yaml realtime enabled default is true")
    void applicationYaml_realtimeEnabledDefault_isTrue() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            assertThat(inputStream).isNotNull();
            String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).contains("enabled: ${CP_CHAT_REALTIME_ENABLED:true}");
        }
    }
}
