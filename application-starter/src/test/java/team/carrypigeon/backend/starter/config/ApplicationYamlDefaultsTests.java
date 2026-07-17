package team.carrypigeon.backend.starter.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * 验证内置配置不再依赖 realtime 环境变量占位。
     */
    @Test
    @DisplayName("application yaml uses direct realtime default")
    void applicationYaml_usesDirectRealtimeDefault() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            assertThat(inputStream).isNotNull();
            String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).contains("realtime:");
            assertThat(yaml).contains("host: 127.0.0.1");
            assertThat(yaml).contains("port: 18080");
            assertThat(yaml).doesNotContain("CP_CHAT_REALTIME_ENABLED");
        }
    }

    /**
     * 验证用户名密码登录在开发默认配置中保持开启。
     */
    @Test
    @DisplayName("application yaml password login enabled default is true")
    void applicationYaml_passwordLoginEnabledDefault_isTrue() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            assertThat(inputStream).isNotNull();
            String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).contains("password-login:");
            assertThat(yaml).contains("enabled: true");
        }
    }

    /**
     * 验证分发包外部配置模板提供完整应用配置入口。
     */
    @Test
    @DisplayName("distribution external yaml includes application runtime config")
    void distributionExternalYaml_includesApplicationRuntimeConfig() throws IOException {
        Path externalConfigPath = Path.of("../distribution/src/config/application.yaml");
        if (!Files.exists(externalConfigPath)) {
            externalConfigPath = Path.of("distribution/src/config/application.yaml");
        }
        String yaml = Files.readString(externalConfigPath, StandardCharsets.UTF_8);

        assertThat(yaml).contains("spring:");
        assertThat(yaml).contains("datasource:");
        assertThat(yaml).contains("redis:");
        assertThat(yaml).contains("jwt:");
        assertThat(yaml).contains("secret: \"\"");
        assertThat(yaml).contains("password-login:");
        assertThat(yaml).contains("enabled: true");
    }

    /**
     * 验证仓库根本地外部配置提供本地调试所需的 JWT 默认值。
     */
    @Test
    @DisplayName("local external yaml includes development jwt secret")
    void localExternalYaml_includesDevelopmentJwtSecret() throws IOException {
        Path localConfigPath = Path.of("../config/application.yaml");
        if (!Files.exists(localConfigPath)) {
            localConfigPath = Path.of("config/application.yaml");
        }
        String yaml = Files.readString(localConfigPath, StandardCharsets.UTF_8);

        assertThat(yaml).contains("secret: carrypigeon-local-dev-jwt-secret-change-me-32");
        assertThat(yaml).contains("spring:");
        assertThat(yaml).contains("datasource:");
        assertThat(yaml).contains("redis:");
    }
}
