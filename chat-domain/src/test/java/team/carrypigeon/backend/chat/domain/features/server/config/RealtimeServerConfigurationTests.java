package team.carrypigeon.backend.chat.domain.features.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.infrastructure.basic.config.BasicInfrastructureAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.id.IdAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.time.TimeAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealtimeServerConfiguration 装配测试。
 * 职责：验证 server feature 是否按配置开关装配 Netty 实时通道运行时。
 * 边界：不实际绑定端口，只验证 Spring Bean 装配边界。
 */
class RealtimeServerConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BasicInfrastructureAutoConfiguration.class,
                    JacksonAutoConfiguration.class,
                    IdAutoConfiguration.class,
                    TimeAutoConfiguration.class,
                    RealtimeServerConfiguration.class
            ))
            .withUserConfiguration(TestSupportConfiguration.class);

    @Configuration
    static class TestSupportConfiguration {

        @Bean
        TimeProvider timeProvider(Clock clock) {
            return new TimeProvider(clock);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        AuthTokenService authTokenService() {
            return new AuthTokenService() {
                @Override
                public String issueAccessToken(AuthAccount account, java.time.Instant expiresAt) {
                    return "access-token";
                }

                @Override
                public String issueRefreshToken(AuthAccount account, long refreshSessionId, java.time.Instant expiresAt) {
                    return "refresh-token";
                }

                @Override
                public AuthTokenClaims parseAccessToken(String accessToken) {
                    return new AuthTokenClaims("1001", "carry-user", "access", 0L, java.time.Instant.parse("2026-04-20T12:30:00Z"));
                }

                @Override
                public AuthTokenClaims parseRefreshToken(String refreshToken) {
                    return new AuthTokenClaims("1001", "carry-user", "refresh", 2001L, java.time.Instant.parse("2026-05-04T12:00:00Z"));
                }
            };
        }
    }

    /**
     * 验证开启实时通道配置时会注册实时通道运行时相关 Bean 定义。
     * 输入：开启实时通道并启用懒加载，避免测试真实绑定端口。
     * 输出：server feature 中存在 Netty 运行时与初始化器 Bean 定义。
     */
    @Test
    @DisplayName("configuration enabled registers runtime bean")
    void configuration_enabled_registersRuntimeBean() {
        contextRunner
                .withPropertyValues(
                        "spring.main.lazy-initialization=true",
                        "cp.chat.server.realtime.enabled=true",
                        "cp.chat.server.realtime.host=127.0.0.1",
                        "cp.chat.server.realtime.port=28080",
                        "cp.chat.server.realtime.path=/ws",
                        "cp.chat.server.realtime.boss-threads=1",
                        "cp.chat.server.realtime.worker-threads=0",
                        "cp.infrastructure.id.worker-id=1",
                        "cp.infrastructure.id.datacenter-id=1"
                )
                .run(context -> {
                    assertThat(context).hasBean("realtimeServerRuntime");
                    assertThat(context).hasBean("realtimeChannelInitializer");
                });
    }

    /**
     * 验证关闭实时通道配置时不会注册实时通道运行时 Bean 定义。
     * 输入：关闭实时通道并启用懒加载。
     * 输出：server feature 中不存在 Netty 运行时 Bean 定义。
     */
    @Test
    @DisplayName("configuration disabled does not register runtime bean")
    void configuration_disabled_doesNotRegisterRuntimeBean() {
        contextRunner
                .withPropertyValues(
                        "spring.main.lazy-initialization=true",
                        "cp.chat.server.realtime.enabled=false",
                        "cp.chat.server.realtime.host=127.0.0.1",
                        "cp.chat.server.realtime.port=28080",
                        "cp.chat.server.realtime.path=/ws",
                        "cp.chat.server.realtime.boss-threads=1",
                        "cp.chat.server.realtime.worker-threads=0",
                        "cp.infrastructure.id.worker-id=1",
                        "cp.infrastructure.id.datacenter-id=1"
                )
                .run(context -> assertThat(context).doesNotHaveBean("realtimeServerRuntime"));
    }
}
