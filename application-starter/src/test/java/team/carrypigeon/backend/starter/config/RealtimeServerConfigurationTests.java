package team.carrypigeon.backend.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.infrastructure.basic.config.BasicInfrastructureAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.id.IdAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.time.TimeAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealtimeServerConfiguration 装配测试。
 * 职责：验证 starter 层是否按配置开关装配 Netty 实时通道运行时。
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
    }

    /**
     * 验证开启实时通道配置时会注册实时通道运行时相关 Bean 定义。
     * 输入：开启实时通道并启用懒加载，避免测试真实绑定端口。
     * 输出：starter 层存在 Netty 运行时与初始化器 Bean 定义。
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
     * 输出：starter 层不存在 Netty 运行时 Bean 定义。
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
