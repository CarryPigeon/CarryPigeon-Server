package team.carrypigeon.backend.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelBansController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelLifecycleController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelMemberGovernanceController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelQueryController;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckRunner;
import team.carrypigeon.tests.starter.support.StarterExternalPortsTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApplicationStarter 启动烟雾测试。
 * 职责：验证真实启动入口的组件扫描、自动配置和关键生产 Bean 唯一性。
 * 边界：关闭真实外部服务并以端口替身隔离环境，不替换领域服务或控制器。
 */
@Tag("smoke")
@SpringBootTest(
        classes = ApplicationStarter.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.banner-mode=off",
                "cp.infrastructure.service.database.enabled=false",
                "cp.infrastructure.service.cache.enabled=false",
                "cp.infrastructure.service.storage.enabled=false",
                "cp.infrastructure.service.mail.enabled=false",
                "cp.chat.server.realtime.enabled=false"
        }
)
@Import(StarterExternalPortsTestConfiguration.class)
class ApplicationStarterSmokeTests {

    @Autowired
    private ApplicationContext context;

    /**
     * 验证真实 ApplicationStarter 在无外部服务环境中完成生产组件装配。
     */
    @Test
    @DisplayName("application starter assembles unique production domain and controller beans")
    void applicationStarter_externalPortsIsolated_assemblesUniqueProductionBeans() {
        assertThat(context.getBeansOfType(InitializationCheckRunner.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuthAccountApi.class)).hasSize(1);
        assertThat(context.getBeansOfType(ChannelMessagePublishingApi.class)).hasSize(1);
        assertThat(context.getBeansOfType(ChannelMessageController.class)).hasSize(1);
        assertThat(context.getBeansOfType(ChannelQueryController.class)).hasSize(1);
        assertThat(context.getBeansOfType(ChannelLifecycleController.class)).hasSize(1);
        assertThat(context.getBeansOfType(ChannelMemberGovernanceController.class)).hasSize(1);
        assertThat(context.getBeansOfType(ChannelBansController.class)).hasSize(1);
    }
}
