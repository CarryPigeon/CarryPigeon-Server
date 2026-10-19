package team.carrypigeon.backend.chat.domain.config.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 验证实时通道配置以配置扫描方式装配时的构造器绑定行为。
 * 说明：本测试守住"不引入 @Configuration 装配类"这条约定，同时确认不可变配置记录必须靠配置扫描绑定。
 */
class RealtimeServerPropertiesBindingTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesScan.class)
            .withPropertyValues(
                    "cp.chat.server.realtime.enabled=true",
                    "cp.chat.server.realtime.host=127.0.0.1",
                    "cp.chat.server.realtime.port=8081",
                    "cp.chat.server.realtime.path=/ws",
                    "cp.chat.server.realtime.boss-threads=1",
                    "cp.chat.server.realtime.worker-threads=2"
            );

    /**
     * 验证组件扫描注册的配置记录能完成构造器绑定，并使用默认值补齐未声明的参数。
     */
    @Test
    void context_componentScannedRecord_bindsConstructorAndDefaults() {
        runner.run(context -> {
            RealtimeServerProperties properties = context.getBean(RealtimeServerProperties.class);

            assertEquals("127.0.0.1", properties.host());
            assertEquals(8081, properties.port());
            assertEquals(10, properties.authenticationTimeoutSeconds());
            assertEquals(Duration.ofHours(1), properties.eventRetention());
            assertEquals("ws://127.0.0.1:8081/ws", properties.wsUrl());
        });
    }

    /**
     * 验证 realtime 关闭时不暴露 websocket 地址，供服务发现返回 null。
     */
    @Test
    void wsUrl_disabled_returnsNull() {
        runner.withPropertyValues("cp.chat.server.realtime.enabled=false").run(context -> {
            assertNull(context.getBean(RealtimeServerProperties.class).wsUrl());
        });
    }

    /**
     * 只扫描实时通道配置记录本身，避免把传输实现一并载入测试上下文。
     */
    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = RealtimeServerProperties.class)
    static class PropertiesScan {
    }
}
