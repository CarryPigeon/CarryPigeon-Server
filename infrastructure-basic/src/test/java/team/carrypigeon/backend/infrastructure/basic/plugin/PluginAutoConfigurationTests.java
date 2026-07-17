package team.carrypigeon.backend.infrastructure.basic.plugin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证插件配置基建自动配置。
 * 职责：确保 Spring Boot 可以注册插件配置属性与统一查询入口。
 * 边界：不装配任何具体插件实现。
 */
@Tag("contract")
class PluginAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class));

    /**
     * 测试默认自动配置。
     * 输入：无 `cp.plugin` 配置。
     * 期望：上下文创建 provider，且没有任何插件被默认启用。
     */
    @Test
    void pluginConfigurationProvider_withoutProperties_registersEmptyProvider() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PluginProperties.class);
            assertThat(context).hasSingleBean(PluginConfigurationProvider.class);
            assertThat(context.getBean(PluginConfigurationProvider.class).isEnabled("translator")).isFalse();
        });
    }

    /**
     * 测试 YAML 风格属性绑定。
     * 输入：启用 translator 插件并提供 options。
     * 期望：provider 可以按插件 ID 读取启停状态与 options。
     */
    @Test
    void pluginConfigurationProvider_withPluginProperties_bindsConfigsAndOptions() {
        contextRunner
                .withPropertyValues(
                        "cp.plugin.configs.translator.enabled=true",
                        "cp.plugin.configs.translator.options.provider=openai",
                        "cp.plugin.configs.translator.options.timeout=5s"
                )
                .run(context -> {
                    PluginConfigurationProvider provider = context.getBean(PluginConfigurationProvider.class);

                    assertThat(provider.isEnabled("translator")).isTrue();
                    assertThat(provider.find("translator")).isPresent();
                    assertThat(provider.find("translator").orElseThrow().options())
                            .containsEntry("provider", "openai")
                            .containsEntry("timeout", "5s");
                });
    }

    /**
     * 测试关闭插件绑定。
     * 输入：显式配置 enabled=false。
     * 期望：插件配置存在，但启用判断仍返回 false。
     */
    @Test
    void pluginConfigurationProvider_withDisabledPlugin_returnsDisabled() {
        contextRunner
                .withPropertyValues("cp.plugin.configs.translator.enabled=false")
                .run(context -> {
                    PluginConfigurationProvider provider = context.getBean(PluginConfigurationProvider.class);

                    assertThat(provider.find("translator")).isPresent();
                    assertThat(provider.isEnabled("translator")).isFalse();
                });
    }
}
