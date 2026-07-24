package example.plugin.messageplugin.mcbridge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;

/**
 * mc-bridge 外部插件自动配置示例。
 *
 * <p>职责：在插件配置开启时导入消息 Domain 注册配置并提供系统插件生命周期 Bean。
 * 边界：示例依赖宿主 plugin feature SPI，不实现独立 ClassLoader 或运行期加载。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "cp.plugin.configs.com-example-mc-bridge",
        name = "enabled",
        havingValue = "true"
)
@Import(McBridgeMessageTypePluginConfiguration.class)
public class McBridgePluginAutoConfiguration {

    @Bean
    public McBridgeSystemPlugin mcBridgeSystemPlugin() {
        return new McBridgeSystemPlugin();
    }
}
