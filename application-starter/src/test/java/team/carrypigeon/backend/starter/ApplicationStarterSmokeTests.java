package team.carrypigeon.backend.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.infrastructure.basic.config.BasicInfrastructureAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.starter.config.InitializationCheckConfiguration;
import team.carrypigeon.backend.starter.config.InitializationCheckRunner;
import team.carrypigeon.backend.starter.support.StarterRegressionConfiguration;
import team.carrypigeon.backend.starter.support.StarterTestRuntimeConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApplicationStarter 启动烟雾测试。
 * 职责：验证 starter 模块在当前消息附件链路场景下的最小装配能力。
 * 边界：不依赖真实外部服务，只验证关键 Bean 的上下文级装配结果。
 */
class ApplicationStarterSmokeTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BasicInfrastructureAutoConfiguration.class,
                    JacksonAutoConfiguration.class
            ))
            .withUserConfiguration(
                    StarterTestRuntimeConfiguration.class,
                    StarterRegressionConfiguration.class,
                    InitializationCheckConfiguration.class
            );

    /**
     * 验证 starter 级上下文能装配消息附件回归所需的关键 Bean。
     */
    @Test
    @DisplayName("starter assembly registers key message attachment beans")
    void starterAssembly_registersKeyMessageAttachmentBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InitializationCheckRunner.class);
            assertThat(context).hasSingleBean(AuthApplicationService.class);
            assertThat(context).hasSingleBean(MessageApplicationService.class);
            assertThat(context).hasSingleBean(ChannelMessageController.class);
            assertThat(context).hasSingleBean(MessageAttachmentObjectKeyPolicy.class);
            assertThat(context).hasSingleBean(MessageAttachmentPayloadResolver.class);
        });
    }
}
